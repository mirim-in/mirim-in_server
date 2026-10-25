package com.mirim.mirimin.notification;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 포인트 지급·회수와 알림 발송을 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class PointAndNotificationIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    JsonMapper jsonMapper;

    MockMvc mvc;
    User questioner;
    User student;
    User teacher;
    User curious;
    User admin;
    Long dormitoryId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        questioner = save("n-questioner@example.com", MemberType.GENERAL, Role.USER);
        student = save("s9904@e-mirim.hs.kr", MemberType.STUDENT, Role.USER);
        teacher = save("n-teacher@e-mirim.hs.kr", MemberType.TEACHER, Role.USER);
        curious = save("n-curious@example.com", MemberType.GENERAL, Role.USER);
        admin = save("n-admin@example.com", MemberType.GENERAL, Role.ADMIN);
        dormitoryId = categoryRepository.findByName("기숙사").orElseThrow().getId();

        // 학생은 기숙사를 답변 가능 분야로 설정
        json(put("/api/users/me/categories"), student, "{\"answerableIds\":[%d]}".formatted(dormitoryId))
                .andExpect(status().isNoContent());
    }

    @Test
    void 질문_답변_채택_댓글에_포인트와_알림이_따라온다() throws Exception {
        String questionId = json(post("/api/questions"), questioner,
                "{\"title\":\"기숙사 통금이 몇 시인가요?\",\"content\":\"궁금해요\",\"categoryIds\":[%d]}".formatted(dormitoryId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertPoint(questioner, 5);

        // 답변 가능 분야 매칭 알림 (교사는 설정하지 않아 받지 않음)
        mvc.perform(get("/api/notifications").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.content[0].type").value("CATEGORY_MATCHED"))
                .andExpect(jsonPath("$.content[0].questionTitle").value("기숙사 통금이 몇 시인가요?"));
        assertUnread(teacher, 0);

        mvc.perform(put("/api/questions/" + questionId + "/curiosity").header("Authorization", bearer(curious)));

        String answerId = json(post("/api/questions/" + questionId + "/answers"), student, "{\"content\":\"10시예요\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertPoint(student, 10);
        // 질문자와 궁금해요 누른 사람이 새 답변 알림을 받는다
        mvc.perform(get("/api/notifications").header("Authorization", bearer(questioner)))
                .andExpect(jsonPath("$.content[0].type").value("ANSWER_CREATED"))
                .andExpect(jsonPath("$.content[0].message").value("테스트님이 답변을 남겼어요."));
        assertUnread(curious, 1);

        mvc.perform(post("/api/answers/" + answerId + "/accept").header("Authorization", bearer(questioner)))
                .andExpect(status().isNoContent());
        assertPoint(student, 40);

        json(post("/api/answers/" + answerId + "/comments"), teacher, "{\"content\":\"정확해요\"}")
                .andExpect(status().isCreated());
        mvc.perform(get("/api/notifications").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.content[0].type").value("COMMENT_CREATED"))
                .andExpect(jsonPath("$.content[1].type").value("ANSWER_ACCEPTED"));
        assertUnread(student, 3);

        // 포인트 내역
        mvc.perform(get("/api/users/me/points").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.content[0].reason").value("ANSWER_ACCEPTED"))
                .andExpect(jsonPath("$.content[0].amount").value(30))
                .andExpect(jsonPath("$.content[1].reason").value("ANSWER_CREATED"));

        // 관리자가 채택된 답변을 지우면 답변·채택 포인트 모두 회수, 질문을 지우면 질문 포인트 회수
        mvc.perform(delete("/api/answers/" + answerId).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        assertPoint(student, 0);
        mvc.perform(delete("/api/questions/" + questionId).header("Authorization", bearer(questioner)))
                .andExpect(status().isNoContent());
        assertPoint(questioner, 0);
    }

    @Test
    void 알림을_하나씩_또는_모두_읽음_처리한다() throws Exception {
        json(post("/api/questions"), questioner,
                "{\"title\":\"질문1\",\"content\":\"내용\",\"categoryIds\":[%d]}".formatted(dormitoryId));
        json(post("/api/questions"), questioner,
                "{\"title\":\"질문2\",\"content\":\"내용\",\"categoryIds\":[%d]}".formatted(dormitoryId));
        assertUnread(student, 2);

        long firstId = jsonMapper.readTree(mvc.perform(get("/api/notifications").header("Authorization", bearer(student)))
                .andReturn().getResponse().getContentAsString()).get("content").get(0).get("id").asLong();
        mvc.perform(patch("/api/notifications/" + firstId + "/read").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        assertUnread(student, 1);

        // 남의 알림은 읽음 처리할 수 없다
        mvc.perform(patch("/api/notifications/" + firstId + "/read").header("Authorization", bearer(teacher)))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/notifications/read-all").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        assertUnread(student, 0);
    }

    @Test
    void 임시_보관_질문은_등록할_때_포인트와_알림이_생긴다() throws Exception {
        String questionId = json(post("/api/questions"), questioner,
                "{\"title\":\"임시\",\"content\":\"내용\",\"categoryIds\":[%d],\"draft\":true}".formatted(dormitoryId))
                .andReturn().getResponse().getContentAsString();
        assertPoint(questioner, 0);
        assertUnread(student, 0);

        json(put("/api/questions/" + questionId), questioner,
                "{\"title\":\"등록\",\"content\":\"내용\",\"categoryIds\":[%d]}".formatted(dormitoryId))
                .andExpect(status().isNoContent());
        assertPoint(questioner, 5);
        assertUnread(student, 1);
    }

    private void assertPoint(User user, int expected) throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.point").value(expected));
    }

    private void assertUnread(User user, int expected) throws Exception {
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.count").value(expected));
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               User user, String body) throws Exception {
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
