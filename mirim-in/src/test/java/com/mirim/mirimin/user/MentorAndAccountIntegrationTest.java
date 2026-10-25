package com.mirim.mirimin.user;

import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.user.entity.AuthProvider;
import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.MemberType;
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

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 멘토, 미림인 승인 신청, 회원 탈퇴를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class MentorAndAccountIntegrationTest {

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

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void 자격이_있는_사용자만_멘토가_되고_멘토_목록에서_분야로_찾는다() throws Exception {
        int thisYear = LocalDate.now().getYear();
        // 학년은 3월에 오르므로 1~2월이면 입학년도를 한 해 당겨서 계산
        int schoolYear = LocalDate.now().getMonthValue() >= 3 ? thisYear : thisYear - 1;
        User freshman = save(student("s9901@e-mirim.hs.kr", schoolYear));
        User sophomore = save(student("s9902@e-mirim.hs.kr", schoolYear - 1));
        User teacher = save(User.builder().email("m-teacher@e-mirim.hs.kr").name("멘토선생님")
                .provider(AuthProvider.LOCAL).memberType(MemberType.TEACHER).build());
        User general = save(User.builder().email("m-general@example.com").name("중학생")
                .provider(AuthProvider.LOCAL).build());
        Long majorId = categoryRepository.findByName("전공").orElseThrow().getId();

        putJson("/api/users/me/mentor", freshman, "{\"enabled\":true}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("U006"));
        putJson("/api/users/me/mentor", general, "{\"enabled\":true}")
                .andExpect(status().isForbidden());

        putJson("/api/users/me/mentor", sophomore, "{\"enabled\":true,\"intro\":\"멘토테스트 포트폴리오 답변 가능해요\"}")
                .andExpect(status().isNoContent());
        putJson("/api/users/me/categories", sophomore, "{\"answerableIds\":[%d]}".formatted(majorId));
        putJson("/api/users/me/mentor", teacher, "{\"enabled\":true,\"intro\":\"멘토테스트 진로 상담\"}")
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/mentors").param("keyword", "멘토테스트"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/mentors").param("keyword", "멘토테스트").param("categoryId", String.valueOf(majorId)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sophomore.getId()))
                .andExpect(jsonPath("$.content[0].intro").value("멘토테스트 포트폴리오 답변 가능해요"))
                .andExpect(jsonPath("$.content[0].answerables[0].name").value("전공"))
                .andExpect(jsonPath("$.content[0].departmentLabel").value("소프트웨어과"));

        // 끄면 목록에서 빠지고, 소개는 유지된다
        putJson("/api/users/me/mentor", teacher, "{\"enabled\":false}").andExpect(status().isNoContent());
        mvc.perform(get("/api/mentors").param("keyword", "멘토테스트"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/users/" + teacher.getId() + "/profile"))
                .andExpect(jsonPath("$.mentorEnabled").value(false))
                .andExpect(jsonPath("$.mentorIntro").value("멘토테스트 진로 상담"));
    }

    @Test
    void 인증받지_않은_사용자만_미림인_승인을_신청한다() throws Exception {
        User general = save(User.builder().email("v-general@example.com").name("졸업생")
                .provider(AuthProvider.LOCAL).build());
        User teacher = save(User.builder().email("v-teacher@e-mirim.hs.kr").name("교사")
                .provider(AuthProvider.LOCAL).memberType(MemberType.TEACHER).build());

        postJson("/api/users/me/verification-requests", general,
                "{\"requestedType\":\"GRADUATE\",\"message\":\"2020년 졸업생입니다\"}")
                .andExpect(status().isCreated());
        postJson("/api/users/me/verification-requests", general, "{\"requestedType\":\"GRADUATE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U008"));
        postJson("/api/users/me/verification-requests", teacher, "{\"requestedType\":\"GRADUATE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U007"));
        postJson("/api/users/me/verification-requests", general, "{\"requestedType\":\"GENERAL\"}")
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/users/me/verification-requests").header("Authorization", bearer(general)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].requestedTypeLabel").value("졸업생"));
    }

    @Test
    void 탈퇴하면_개인정보가_지워지고_글은_탈퇴한_사용자로_남으며_같은_이메일로_재가입된다() throws Exception {
        String email = "s9903@e-mirim.hs.kr";
        String accessToken = jsonMapper.readTree(mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"탈퇴예정\",\"email\":\"%s\",\"password\":\"mirim1234\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("accessToken").asString();
        User asker = save(User.builder().email("w-asker@example.com").name("질문자").provider(AuthProvider.LOCAL).build());

        String questionId = postJson("/api/questions", asker, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/questions/" + questionId + "/answers").header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"탈퇴 전 답변\"}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
        mvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"mirim1234\"}"))
                .andExpect(status().isNoContent());

        // 기존 토큰으로는 아무것도 할 수 없고, 로그인도 안 된다
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"mirim1234\"}".formatted(email)))
                .andExpect(status().isUnauthorized());

        // 답변은 남고 작성자는 탈퇴한 사용자
        mvc.perform(get("/api/questions/" + questionId + "/answers"))
                .andExpect(jsonPath("$[0].content").value("탈퇴 전 답변"))
                .andExpect(jsonPath("$[0].author.withdrawn").value(true))
                .andExpect(jsonPath("$[0].author.name").value("탈퇴한 사용자"));

        // 같은 이메일로 다시 가입할 수 있다
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"재가입\",\"email\":\"%s\",\"password\":\"mirim1234\"}".formatted(email)))
                .andExpect(status().isCreated());
    }

    private User student(String email, int admissionYear) {
        return User.builder().email(email).name("학생").provider(AuthProvider.LOCAL)
                .memberType(MemberType.STUDENT).department(Department.SOFTWARE).admissionYear(admissionYear).build();
    }

    private User save(User user) {
        return userRepository.save(user);
    }

    private ResultActions putJson(String url, User user, String body) throws Exception {
        return mvc.perform(put(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postJson(String url, User user, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
