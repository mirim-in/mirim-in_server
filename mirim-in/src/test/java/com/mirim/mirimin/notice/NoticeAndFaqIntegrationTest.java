package com.mirim.mirimin.notice;

import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.user.entity.AuthProvider;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.Role;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 공지와 FAQ를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class NoticeAndFaqIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User admin;
    User user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = userRepository.save(User.builder().email("nf-admin@example.com").name("운영자")
                .provider(AuthProvider.LOCAL).role(Role.ADMIN).build());
        user = userRepository.save(User.builder().email("nf-user@example.com").name("사용자")
                .provider(AuthProvider.LOCAL).memberType(MemberType.GENERAL).build());
    }

    @Test
    void 임시_보관_공지는_숨겨지고_등록할_때_전체_알림이_한_번_간다() throws Exception {
        json(post("/api/admin/notices"), user, "{\"title\":\"공지\",\"content\":\"내용\"}")
                .andExpect(status().isForbidden());

        String noticeId = json(post("/api/admin/notices"), admin,
                "{\"title\":\"공지테스트 시험 기간 안내\",\"content\":\"내용\",\"pinned\":true,\"draft\":true}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        // 임시 보관: 사용자에게 안 보이고 알림도 없음
        mvc.perform(get("/api/notices/" + noticeId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/notices/pinned"))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(Integer.valueOf(noticeId)))));
        mvc.perform(get("/api/admin/notices").param("status", "DRAFT").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(noticeId))));
        assertUnread(user, 0);

        // 등록 → 알림
        String publish = "{\"title\":\"공지테스트 시험 기간 안내\",\"content\":\"10월 30일부터\",\"pinned\":true}";
        json(put("/api/admin/notices/" + noticeId), admin, publish).andExpect(status().isNoContent());
        assertUnread(user, 1);
        assertUnread(admin, 0);
        mvc.perform(get("/api/notifications").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.content[0].type").value("NOTICE"))
                .andExpect(jsonPath("$.content[0].noticeId").value(Integer.valueOf(noticeId)))
                .andExpect(jsonPath("$.content[0].message").value("새 공지: 공지테스트 시험 기간 안내"));

        // 고정 공지, 공지 목록 맨 위
        mvc.perform(get("/api/notices/pinned"))
                .andExpect(jsonPath("$[*].id").value(hasItem(Integer.valueOf(noticeId))));
        mvc.perform(get("/api/notices"))
                .andExpect(jsonPath("$.content[0].id").value(Integer.valueOf(noticeId)));
        mvc.perform(get("/api/notices/" + noticeId))
                .andExpect(jsonPath("$.content").value("10월 30일부터"))
                .andExpect(jsonPath("$.authorName").value("운영자"));

        // 등록된 공지를 고쳐도 알림은 다시 가지 않는다
        json(put("/api/admin/notices/" + noticeId), admin, publish).andExpect(status().isNoContent());
        assertUnread(user, 1);

        mvc.perform(delete("/api/admin/notices/" + noticeId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notices/" + noticeId)).andExpect(status().isNotFound());
    }

    @Test
    void FAQ를_등록하고_카테고리별로_조회하고_수정_삭제한다() throws Exception {
        Long dormitoryId = categoryRepository.findByName("기숙사").orElseThrow().getId();

        String dormFaq = json(post("/api/admin/faqs"), admin,
                "{\"categoryId\":%d,\"question\":\"FAQ테스트 기숙사 통금은?\",\"answer\":\"10시\",\"sortOrder\":2}"
                        .formatted(dormitoryId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String generalFaq = json(post("/api/admin/faqs"), admin,
                "{\"question\":\"FAQ테스트 면접 질문은?\",\"answer\":\"자기소개\",\"sortOrder\":1}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/faqs").param("categoryId", String.valueOf(dormitoryId)))
                .andExpect(jsonPath("$[*].id").value(hasItem(Integer.valueOf(dormFaq))))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(Integer.valueOf(generalFaq)))));
        mvc.perform(get("/api/faqs"))
                .andExpect(jsonPath("$[*].question").value(hasItem("FAQ테스트 면접 질문은?")));

        // 공식 카테고리가 아니면 거부
        json(post("/api/admin/faqs"), admin, "{\"categoryId\":999999999,\"question\":\"q\",\"answer\":\"a\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G001"));

        json(put("/api/admin/faqs/" + dormFaq), admin,
                "{\"categoryId\":%d,\"question\":\"FAQ테스트 기숙사 통금은?\",\"answer\":\"10시 30분\"}"
                        .formatted(dormitoryId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/faqs").param("categoryId", String.valueOf(dormitoryId)))
                .andExpect(jsonPath("$[?(@.id == %s)].answer".formatted(dormFaq)).value("10시 30분"))
                .andExpect(jsonPath("$[?(@.id == %s)].sortOrder".formatted(dormFaq)).value(2));

        mvc.perform(delete("/api/admin/faqs/" + dormFaq).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/faqs"))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(Integer.valueOf(dormFaq)))));
    }

    private void assertUnread(User target, int expected) throws Exception {
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(target)))
                .andExpect(jsonPath("$.count").value(expected));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, User actor, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String bearer(User target) {
        return "Bearer " + jwtProvider.createAccessToken(target.getId(), target.getRole());
    }
}
