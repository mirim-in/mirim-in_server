package com.mirim.mirimin.auth;

import com.mirim.mirimin.user.entity.VerificationPurpose;
import com.mirim.mirimin.user.repository.EmailVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 DB(.env의 DB_*)에 붙어 인증 API 전체 흐름을 확인한다. 테스트가 끝나면 롤백된다.
 * 이메일 인증 필수 모드로 실행한다. (생략 모드는 EmailVerificationOptionalTest)
 * 메일 설정이 비어 있으면 코드가 발송되지 않으므로 DB에서 코드를 꺼내 쓴다.
 */
@SpringBootTest(properties = "app.auth.email-verification-required=true")
@Transactional
class AuthFlowIntegrationTest {

    private static final String STUDENT_EMAIL = "s2599@e-mirim.hs.kr";
    private static final String PASSWORD = "mirim1234";

    @Autowired
    WebApplicationContext context;
    @Autowired
    EmailVerificationRepository verificationRepository;
    @Autowired
    JsonMapper jsonMapper;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void 학교_이메일로_가입하면_재학생으로_인증되고_로그인_재발급_로그아웃까지_된다() throws Exception {
        // 인증 없이 가입하면 거부
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'테스트','email':'%s','password':'%s'}", STUDENT_EMAIL, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A003"));

        mvc.perform(post("/api/auth/email/code").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s'}", STUDENT_EMAIL)))
                .andExpect(status().isNoContent());

        // 60초 안에 다시 요청하면 거부
        mvc.perform(post("/api/auth/email/code").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s'}", STUDENT_EMAIL)))
                .andExpect(status().isTooManyRequests());

        mvc.perform(post("/api/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','code':'000000'}", STUDENT_EMAIL)))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.oneOf("A004")));

        String code = verificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(STUDENT_EMAIL, VerificationPurpose.SIGNUP)
                .orElseThrow().getCode();
        mvc.perform(post("/api/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','code':'%s'}", STUDENT_EMAIL, code)))
                .andExpect(status().isNoContent());

        // 비밀번호 규칙 위반
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'테스트','email':'%s','password':'short'}", STUDENT_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'테스트','email':'%s','password':'%s'}", STUDENT_EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 중복 가입
        mvc.perform(post("/api/auth/email/code").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s'}", STUDENT_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U001"));

        // 틀린 비밀번호
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'wrong1234'}", STUDENT_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));

        JsonNode tokens = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'%s','password':'%s'}", STUDENT_EMAIL.toUpperCase(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn());
        String accessToken = tokens.get("accessToken").asString();
        String refreshToken = tokens.get("refreshToken").asString();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberType").value("STUDENT"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.department").value("SOFTWARE"))
                .andExpect(jsonPath("$.role").value("USER"));

        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C002"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer broken.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A002"));

        // 일반 사용자는 관리자 API 거부
        mvc.perform(get("/api/admin/anything").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C003"));

        // 재발급 후 이전 Refresh Token은 다시 쓸 수 없다
        String newRefreshToken = body(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'%s'}", refreshToken)))
                .andExpect(status().isOk())
                .andReturn()).get("refreshToken").asString();
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'%s'}", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A002"));

        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'%s'}", newRefreshToken)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'refreshToken':'%s'}", newRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 일반_이메일은_일반_사용자로_가입된다() throws Exception {
        String email = "middle-school-test@example.com";
        mvc.perform(post("/api/auth/email/code").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'%s'}", email))).andExpect(status().isNoContent());
        String code = verificationRepository.findTopByEmailAndPurposeOrderByIdDesc(email, VerificationPurpose.SIGNUP)
                .orElseThrow().getCode();
        mvc.perform(post("/api/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'%s','code':'%s'}", email, code))).andExpect(status().isNoContent());

        String accessToken = body(mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'name':'중학생','email':'%s','password':'%s'}", email, PASSWORD)))
                .andExpect(status().isCreated()).andReturn()).get("accessToken").asString();

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.memberType").value("GENERAL"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.canBeMentor").value(false));
    }

    @Test
    void 비밀번호_재설정은_가입하지_않은_이메일에도_204를_준다() throws Exception {
        mvc.perform(post("/api/auth/password/reset-request").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'nobody@example.com'}")))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'token':'invalid','newPassword':'%s'}", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A009"));
    }

    private String json(String template, Object... args) {
        return String.format(template, args).replace('\'', '"');
    }

    private JsonNode body(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }
}
