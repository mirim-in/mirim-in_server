package com.mirim.mirimin.admin;

import com.mirim.mirimin.auth.jwt.JwtProvider;
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
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 신고 처리, 콘텐츠·카테고리 관리를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class AdminContentIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    JsonMapper jsonMapper;

    MockMvc mvc;
    User admin;
    User asker;
    User student;
    User reporter;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = save("ac-admin@example.com", MemberType.GENERAL, Role.ADMIN);
        asker = save("ac-asker@example.com", MemberType.GENERAL, Role.USER);
        student = save("s9907@e-mirim.hs.kr", MemberType.STUDENT, Role.USER);
        reporter = save("ac-reporter@example.com", MemberType.GENERAL, Role.USER);
    }

    @Test
    void 신고를_보류했다가_정지와_삭제로_처리하면_같은_글의_신고가_모두_완료된다() throws Exception {
        String questionId = json(post("/api/questions"), asker, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andReturn().getResponse().getContentAsString();
        String answerId = json(post("/api/questions/" + questionId + "/answers"), student, "{\"content\":\"나쁜 답변\"}")
                .andReturn().getResponse().getContentAsString();
        String reportBody = "{\"targetType\":\"ANSWER\",\"targetId\":%s,\"reasons\":[\"SLANDER\",\"PROFANITY\"]}"
                .formatted(answerId);
        String firstReport = json(post("/api/reports"), reporter, reportBody)
                .andReturn().getResponse().getContentAsString();
        String secondReport = json(post("/api/reports"), asker, reportBody)
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/admin/reports").param("status", "PENDING").param("targetType", "ANSWER")
                        .param("reason", "SLANDER").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(firstReport))))
                .andExpect(jsonPath("$.content[0].questionId").value(Integer.valueOf(questionId)))
                .andExpect(jsonPath("$.content[0].reasonLabels[0]").value("비방"))
                .andExpect(jsonPath("$.content[0].contentSnapshot").value("나쁜 답변"))
                .andExpect(jsonPath("$.content[0].targetUserEmail").value("s9907@e-mirim.hs.kr"))
                .andExpect(jsonPath("$.content[0].targetDeleted").value(false));

        json(patch("/api/admin/reports/" + firstReport), admin, "{\"status\":\"ON_HOLD\"}")
                .andExpect(status().isNoContent());
        json(patch("/api/admin/reports/" + firstReport), admin, "{\"status\":\"PENDING\"}")
                .andExpect(status().isBadRequest());

        // 처리 완료 + 7일 정지 + 답변 삭제
        json(patch("/api/admin/reports/" + firstReport), admin,
                "{\"status\":\"DONE\",\"suspendDays\":7,\"deleteContent\":true}")
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/reports").param("status", "DONE").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(firstReport))))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(secondReport))))
                .andExpect(jsonPath("$.content[0].targetDeleted").value(true));
        mvc.perform(get("/api/questions/" + questionId + "/answers"))
                .andExpect(jsonPath("$.length()").value(0));
        json(post("/api/questions"), student, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("U004"));
    }

    @Test
    void 관리자_질문_목록은_익명_질문의_실제_작성자를_보여주고_삭제할_수_있다() throws Exception {
        String questionId = json(post("/api/questions"), asker,
                "{\"title\":\"관리자익명테스트\",\"content\":\"내용\",\"anonymous\":true}")
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/admin/questions").param("keyword", "관리자익명테스트").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[0].question.author").doesNotExist())
                .andExpect(jsonPath("$.content[0].realAuthor.id").value(asker.getId()));

        mvc.perform(delete("/api/admin/questions/" + questionId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId)).andExpect(status().isNotFound());
    }

    @Test
    void 카테고리를_추가_전환_수정_삭제한다() throws Exception {
        // 사용자가 만든 카테고리
        String questionId = json(post("/api/questions"), asker,
                "{\"title\":\"질문\",\"content\":\"내용\",\"newCategories\":[\"관리테스트키워드\"]}")
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/admin/categories").param("official", "false").param("keyword", "관리테스트키워드")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[0].questionCount").value(1));

        // 같은 이름으로 추가하면 공식으로 전환
        String userCategoryId = json(post("/api/admin/categories"), admin, "{\"name\":\"관리테스트키워드\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[*].name").value(hasItem("관리테스트키워드")));
        json(post("/api/admin/categories"), admin, "{\"name\":\"관리테스트키워드\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("G003"));

        // 이름·순서 수정
        json(put("/api/admin/categories/" + userCategoryId), admin, "{\"name\":\"관리테스트수정\",\"sortOrder\":0}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[0].name").value("관리테스트수정"));

        // 삭제하면 공식 목록과 질문 상세에서 사라진다
        mvc.perform(delete("/api/admin/categories/" + userCategoryId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/categories"))
                .andExpect(jsonPath("$[*].name").value(not(hasItem("관리테스트수정"))));
        mvc.perform(get("/api/questions/" + questionId))
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, User user, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User save(String email, MemberType type, Role role) {
        return userRepository.save(User.builder().email(email).name("테스트").provider(AuthProvider.LOCAL)
                .memberType(type).role(role).build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
