package com.mirim.mirimin.admin;

import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.user.entity.AuthProvider;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 포인트 정책·수동 조정·관리자 로그를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class AdminPointIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User admin;
    User user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = userRepository.save(User.builder().email("ap-admin@example.com").name("운영자")
                .provider(AuthProvider.LOCAL).role(Role.ADMIN).build());
        user = userRepository.save(User.builder().email("ap-user@example.com").name("사용자")
                .provider(AuthProvider.LOCAL).build());
    }

    @Test
    void 포인트_정책을_바꾸면_이후_활동부터_적용된다() throws Exception {
        mvc.perform(get("/api/admin/point-policies").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].action").value("QUESTION"))
                .andExpect(jsonPath("$[0].label").value("질문 작성"))
                .andExpect(jsonPath("$[0].defaultAmount").value(5));

        json(put("/api/admin/point-policies/QUESTION"), admin, "{\"amount\":7}").andExpect(status().isNoContent());
        json(put("/api/admin/point-policies/QUESTION"), admin, "{\"amount\":-1}").andExpect(status().isBadRequest());
        json(put("/api/admin/point-policies/QUESTION"), user, "{\"amount\":100}").andExpect(status().isForbidden());

        json(post("/api/questions"), user, "{\"title\":\"질문\",\"content\":\"내용\"}").andExpect(status().isCreated());
        assertPoint(user, 7);

        mvc.perform(get("/api/admin/logs").param("action", "UPDATE_POINT_POLICY").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.content[0].actionLabel").value("포인트 정책 변경"))
                .andExpect(jsonPath("$.content[0].detail").value("질문 작성 5 → 7"))
                .andExpect(jsonPath("$.content[0].adminName").value("운영자"));
    }

    @Test
    void 관리자가_포인트를_지급하고_차감하면_내역에_사유가_남는다() throws Exception {
        json(post("/api/admin/users/" + user.getId() + "/points"), admin, "{\"amount\":100,\"memo\":\"행사 보상\"}")
                .andExpect(status().isNoContent());
        json(post("/api/admin/users/" + user.getId() + "/points"), admin, "{\"amount\":-20,\"memo\":\"부적절 활동\"}")
                .andExpect(status().isNoContent());
        assertPoint(user, 80);

        // 사용자 본인 내역에도 사유가 보인다
        mvc.perform(get("/api/users/me/points").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.content[0].amount").value(-20))
                .andExpect(jsonPath("$.content[0].reasonLabel").value("관리자 조정"))
                .andExpect(jsonPath("$.content[0].memo").value("부적절 활동"))
                .andExpect(jsonPath("$.content[1].memo").value("행사 보상"));
        mvc.perform(get("/api/admin/users/" + user.getId() + "/points").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(2));

        json(post("/api/admin/users/" + user.getId() + "/points"), admin, "{\"amount\":0,\"memo\":\"0\"}")
                .andExpect(status().isBadRequest());
        json(post("/api/admin/users/" + user.getId() + "/points"), admin, "{\"amount\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("memo"));
        json(post("/api/admin/users/999999999/points"), admin, "{\"amount\":10,\"memo\":\"없는 회원\"}")
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/admin/logs").param("action", "ADJUST_POINT").param("targetType", "USER")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].detail").value("-20 - 부적절 활동"))
                .andExpect(jsonPath("$.content[0].targetId").value(user.getId()));
    }

    private void assertPoint(User target, int expected) throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearer(target)))
                .andExpect(jsonPath("$.point").value(expected));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, User actor, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String bearer(User target) {
        return "Bearer " + jwtProvider.createAccessToken(target.getId(), target.getRole());
    }
}
