package com.mirim.mirimin.admin;

import com.mirim.mirimin.admin.repository.AdminLogRepository;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 관리자 대시보드·회원 관리·미림인 승인을 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class AdminUserIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    AdminLogRepository adminLogRepository;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    JsonMapper jsonMapper;

    MockMvc mvc;
    User admin;
    User student;
    User general;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = save("ad-admin@example.com", "관리자", MemberType.GENERAL, Role.ADMIN);
        student = save("s9906@e-mirim.hs.kr", "관리테스트학생", MemberType.STUDENT, Role.USER);
        general = save("ad-general@example.com", "관리테스트일반", MemberType.GENERAL, Role.USER);
    }

    @Test
    void 관리자가_아니면_관리자_API를_쓸_수_없다() throws Exception {
        mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C003"));
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void 대시보드_수치가_활동에_따라_바뀐다() throws Exception {
        JsonNode before = body(mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(admin))));

        String questionId = json(post("/api/questions"), general, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andReturn().getResponse().getContentAsString();
        String answerId = json(post("/api/questions/" + questionId + "/answers"), student, "{\"content\":\"답변\"}")
                .andReturn().getResponse().getContentAsString();
        json(post("/api/reports"), general,
                "{\"targetType\":\"ANSWER\",\"targetId\":%s,\"reasons\":[\"RUDE\"]}".formatted(answerId));
        json(post("/api/users/me/verification-requests"), general, "{\"requestedType\":\"GRADUATE\"}");

        JsonNode after = body(mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(admin))));
        assertThat(after.get("questionCount").asLong() - before.get("questionCount").asLong()).isEqualTo(1);
        assertThat(after.get("answerCount").asLong() - before.get("answerCount").asLong()).isEqualTo(1);
        assertThat(after.get("pendingReports").asLong() - before.get("pendingReports").asLong()).isEqualTo(1);
        assertThat(after.get("pendingVerifications").asLong() - before.get("pendingVerifications").asLong())
                .isEqualTo(1);
        assertThat(after.get("verifiedUsers").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void 회원을_검색하고_정지_해제_강제_탈퇴한다() throws Exception {
        // 받은 신고 수
        String questionId = json(post("/api/questions"), general, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andReturn().getResponse().getContentAsString();
        String answerId = json(post("/api/questions/" + questionId + "/answers"), student, "{\"content\":\"답변\"}")
                .andReturn().getResponse().getContentAsString();
        json(post("/api/reports"), general,
                "{\"targetType\":\"ANSWER\",\"targetId\":%s,\"reasons\":[\"RUDE\"]}".formatted(answerId));

        mvc.perform(get("/api/admin/users").param("keyword", "관리테스트").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/users").param("keyword", "관리테스트").param("memberType", "STUDENT")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("s9906@e-mirim.hs.kr"))
                .andExpect(jsonPath("$.content[0].reportCount").value(1))
                .andExpect(jsonPath("$.content[0].suspended").value(false));

        // 3일 정지 → 글쓰기 불가, 정지 회원 목록에 나옴
        json(put("/api/admin/users/" + student.getId() + "/suspension"), admin, "{\"days\":3,\"reason\":\"비방\"}")
                .andExpect(status().isNoContent());
        json(post("/api/questions"), student, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("U004"));
        mvc.perform(get("/api/admin/users").param("keyword", "관리테스트").param("suspended", "true")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].suspendedUntil").isNotEmpty());

        // 해제 → 다시 글쓰기 가능
        mvc.perform(delete("/api/admin/users/" + student.getId() + "/suspension").header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        json(post("/api/questions"), student, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andExpect(status().isCreated());

        // 영구 정지는 suspendedUntil 없음
        json(put("/api/admin/users/" + general.getId() + "/suspension"), admin, "{}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/users").param("keyword", "관리테스트일반").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[0].suspended").value(true))
                .andExpect(jsonPath("$.content[0].suspendedUntil").doesNotExist());

        // 관리자 계정에는 적용 불가
        json(put("/api/admin/users/" + admin.getId() + "/suspension"), admin, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("D001"));

        // 강제 탈퇴 → 목록에서 빠진다
        mvc.perform(delete("/api/admin/users/" + general.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/users").param("keyword", "관리테스트").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));

        assertThat(adminLogRepository.count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void 미림인_승인_신청을_승인하거나_반려한다() throws Exception {
        String requestId = json(post("/api/users/me/verification-requests"), general,
                "{\"requestedType\":\"GRADUATE\",\"message\":\"2020년 졸업생\"}")
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/admin/verification-requests").param("keyword", "관리테스트일반")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[0].id").value(Integer.valueOf(requestId)))
                .andExpect(jsonPath("$.content[0].requestedTypeLabel").value("졸업생"))
                .andExpect(jsonPath("$.content[0].message").value("2020년 졸업생"));

        mvc.perform(post("/api/admin/verification-requests/" + requestId + "/approve")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").header("Authorization", bearer(general)))
                .andExpect(jsonPath("$.memberType").value("GRADUATE"))
                .andExpect(jsonPath("$.verified").value(true));
        mvc.perform(post("/api/admin/verification-requests/" + requestId + "/approve")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("D003"));

        // 반려
        User another = save("ad-another@example.com", "반려대상", MemberType.GENERAL, Role.USER);
        String rejectId = json(post("/api/users/me/verification-requests"), another, "{\"requestedType\":\"TEACHER\"}")
                .andReturn().getResponse().getContentAsString();
        json(post("/api/admin/verification-requests/" + rejectId + "/reject"), admin, "{\"reason\":\"증빙 부족\"}")
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me/verification-requests").header("Authorization", bearer(another)))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectReason").value("증빙 부족"));
        mvc.perform(get("/api/users/me").header("Authorization", bearer(another)))
                .andExpect(jsonPath("$.memberType").value("GENERAL"));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, User user, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User save(String email, String name, MemberType type, Role role) {
        return userRepository.save(User.builder().email(email).name(name).provider(AuthProvider.LOCAL)
                .memberType(type).role(role).build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
