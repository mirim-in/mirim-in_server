package com.mirim.mirimin.user;

import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.image.service.ImagePurpose;
import com.mirim.mirimin.image.service.ImageUploader;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 프로필 API를 확인한다. 이미지 업로드만 가짜로 바꾼다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class ProfileIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    JwtProvider jwtProvider;
    @MockitoBean
    ImageUploader imageUploader;

    MockMvc mvc;
    User student;
    User general;
    Long dormitoryId;
    Long majorId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        student = userRepository.save(User.builder().email("s2595@e-mirim.hs.kr").name("학생")
                .provider(AuthProvider.LOCAL).memberType(MemberType.STUDENT)
                .department(Department.SOFTWARE).admissionYear(2025).build());
        general = userRepository.save(User.builder().email("p-general@example.com").name("중학생")
                .provider(AuthProvider.LOCAL).build());
        dormitoryId = categoryRepository.findByName("기숙사").orElseThrow().getId();
        majorId = categoryRepository.findByName("전공").orElseThrow().getId();
    }

    @Test
    void 프로필을_수정하고_분야와_기술_스택을_설정한다() throws Exception {
        // 재학생은 학과를 바꿀 수 없다
        putJson("/api/users/me/profile", student,
                "{\"name\":\"강별\",\"classNo\":5,\"bio\":\"안녕하세요\",\"department\":\"DESIGN\"}")
                .andExpect(status().isNoContent());
        putJson("/api/users/me/categories", student,
                "{\"interestIds\":[%d],\"answerableIds\":[%d,%d]}".formatted(majorId, dormitoryId, majorId))
                .andExpect(status().isNoContent());
        putJson("/api/users/me/tech-stacks", student, "{\"names\":[\"Java\",\"java\",\"React\"]}")
                .andExpect(status().isNoContent());

        // 다른 사람이 보면 이메일은 없다
        mvc.perform(get("/api/users/" + student.getId() + "/profile").header("Authorization", bearer(general)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("강별"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.department").value("SOFTWARE"))
                .andExpect(jsonPath("$.classNo").value(5))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.interests[0].name").value("전공"))
                .andExpect(jsonPath("$.answerables.length()").value(2))
                .andExpect(jsonPath("$.techStacks.length()").value(2))
                .andExpect(jsonPath("$.techStacks[0]").value("Java"))
                .andExpect(jsonPath("$.mine").value(false));
        mvc.perform(get("/api/users/" + student.getId() + "/profile").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.email").value("s2595@e-mirim.hs.kr"))
                .andExpect(jsonPath("$.mine").value(true));

        // 목록을 다시 보내면 통째로 바뀐다
        putJson("/api/users/me/categories", student, "{\"interestIds\":[],\"answerableIds\":[%d]}".formatted(majorId))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/" + student.getId() + "/profile"))
                .andExpect(jsonPath("$.interests.length()").value(0))
                .andExpect(jsonPath("$.answerables.length()").value(1));
    }

    @Test
    void 일반_사용자는_답변_가능_분야를_설정할_수_없고_공식_카테고리만_고를_수_있다() throws Exception {
        putJson("/api/users/me/categories", general, "{\"answerableIds\":[%d]}".formatted(dormitoryId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("U005"));
        putJson("/api/users/me/categories", general, "{\"interestIds\":[%d]}".formatted(dormitoryId))
                .andExpect(status().isNoContent());
        putJson("/api/users/me/categories", general, "{\"interestIds\":[999999999]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G001"));
    }

    @Test
    void 활동_통계와_대표_답변이_나오고_익명_질문은_다른_사람에게_숨겨진다() throws Exception {
        putJson("/api/users/me/tech-stacks", student, "{\"names\":[\"Spring\"]}");
        String questionId = postJson("/api/questions", general, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andReturn().getResponse().getContentAsString();
        String answerId = postJson("/api/questions/" + questionId + "/answers", student, "{\"content\":\"답변입니다\"}")
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/answers/" + answerId + "/accept").header("Authorization", bearer(general)));
        putJson("/api/answers/" + answerId + "/vote", general, "{\"type\":\"LIKE\"}");

        // 학생이 익명 질문 1개, 일반 질문 1개
        postJson("/api/questions", student, "{\"title\":\"익명\",\"content\":\"내용\",\"anonymous\":true}");
        postJson("/api/questions", student, "{\"title\":\"공개\",\"content\":\"내용\"}");

        mvc.perform(get("/api/users/" + student.getId() + "/profile"))
                .andExpect(jsonPath("$.stats.questionCount").value(1))
                .andExpect(jsonPath("$.stats.answerCount").value(1))
                .andExpect(jsonPath("$.stats.acceptedCount").value(1))
                .andExpect(jsonPath("$.stats.acceptRate").value(100))
                .andExpect(jsonPath("$.stats.likeCount").value(1))
                .andExpect(jsonPath("$.representativeAnswers[0].answerId").value(Integer.valueOf(answerId)))
                .andExpect(jsonPath("$.representativeAnswers[0].questionTitle").value("질문"));
        mvc.perform(get("/api/users/" + student.getId() + "/profile").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.stats.questionCount").value(2));
        mvc.perform(get("/api/users/" + student.getId() + "/questions"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("공개"));
        mvc.perform(get("/api/users/" + student.getId() + "/answers"))
                .andExpect(jsonPath("$.content[0].accepted").value(true));

        // 답변 목록에 답변자의 기술 스택이 나온다 (AN-004)
        mvc.perform(get("/api/questions/" + questionId + "/answers"))
                .andExpect(jsonPath("$[0].authorTechStacks[0]").value("Spring"));
    }

    @Test
    void 프로필_사진을_올린다() throws Exception {
        given(imageUploader.upload(any(), eq(ImagePurpose.PROFILE)))
                .willReturn("https://res.cloudinary.com/demo/profile/a.png");

        mvc.perform(multipart("/api/users/me/profile-image")
                        .file(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3}))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/demo/profile/a.png"));
        mvc.perform(get("/api/users/" + student.getId() + "/profile"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://res.cloudinary.com/demo/profile/a.png"));

        mvc.perform(multipart("/api/images")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1}))
                        .header("Authorization", bearer(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("I001"));
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
