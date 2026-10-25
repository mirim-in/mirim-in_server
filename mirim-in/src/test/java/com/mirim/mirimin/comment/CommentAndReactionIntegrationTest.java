package com.mirim.mirimin.comment;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 댓글, 저도 궁금해요, 즐겨찾기를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class CommentAndReactionIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User questioner;
    User student;
    User admin;
    String questionId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        questioner = userRepository.save(user("c-questioner@example.com", MemberType.GENERAL, Role.USER));
        student = userRepository.save(user("s2596@e-mirim.hs.kr", MemberType.STUDENT, Role.USER));
        admin = userRepository.save(user("c-admin@example.com", MemberType.GENERAL, Role.ADMIN));

        // 익명 질문
        questionId = mvc.perform(post("/api/questions").header("Authorization", bearer(questioner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"익명 질문\",\"content\":\"내용\",\"anonymous\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 질문과_답변에_댓글을_달고_익명_질문자의_댓글은_익명으로_보인다() throws Exception {
        // 일반 사용자도 댓글 가능
        String questionerComment = comment("/api/questions/" + questionId + "/comments", questioner, "보충 설명이에요")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        comment("/api/questions/" + questionId + "/comments", student, "어떤 학과인가요?")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/questions/" + questionId + "/comments").header("Authorization", bearer(questioner)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].anonymous").value(true))
                .andExpect(jsonPath("$[0].author").doesNotExist())
                .andExpect(jsonPath("$[0].mine").value(true))
                .andExpect(jsonPath("$[1].anonymous").value(false))
                .andExpect(jsonPath("$[1].author.memberTypeLabel").value("재학생"));

        // 답변 댓글은 질문 댓글 목록에 섞이지 않는다
        String answerId = mvc.perform(post("/api/questions/" + questionId + "/answers")
                        .header("Authorization", bearer(student)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"답변\"}"))
                .andReturn().getResponse().getContentAsString();
        comment("/api/answers/" + answerId + "/comments", questioner, "감사합니다").andExpect(status().isCreated());
        mvc.perform(get("/api/answers/" + answerId + "/comments"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].anonymous").value(true));
        mvc.perform(get("/api/questions/" + questionId + "/comments"))
                .andExpect(jsonPath("$.length()").value(2));

        // 수정은 작성자만, 삭제는 작성자 또는 관리자
        mvc.perform(put("/api/comments/" + questionerComment).header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"남의 댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("M002"));
        mvc.perform(put("/api/comments/" + questionerComment).header("Authorization", bearer(questioner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정한 댓글\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/comments/" + questionerComment).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId + "/comments"))
                .andExpect(jsonPath("$.length()").value(1));

        comment("/api/questions/" + questionId + "/comments", null, "비로그인")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 저도_궁금해요와_즐겨찾기를_누르고_취소한다() throws Exception {
        mvc.perform(put("/api/questions/" + questionId + "/curiosity").header("Authorization", bearer(questioner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Q003"));

        // 두 번 눌러도 한 번만 센다
        mvc.perform(put("/api/questions/" + questionId + "/curiosity").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/questions/" + questionId + "/curiosity").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/questions/" + questionId + "/bookmark").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/questions/" + questionId).header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.curiosityCount").value(1))
                .andExpect(jsonPath("$.curious").value(true))
                .andExpect(jsonPath("$.bookmarked").value(true));
        mvc.perform(get("/api/questions/" + questionId))
                .andExpect(jsonPath("$.curious").value(false))
                .andExpect(jsonPath("$.bookmarked").value(false));

        mvc.perform(get("/api/users/me/bookmarks").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(Integer.valueOf(questionId)));

        mvc.perform(delete("/api/questions/" + questionId + "/curiosity").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/questions/" + questionId + "/bookmark").header("Authorization", bearer(student)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId).header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.curiosityCount").value(0))
                .andExpect(jsonPath("$.curious").value(false))
                .andExpect(jsonPath("$.bookmarked").value(false));
        mvc.perform(get("/api/users/me/bookmarks").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private ResultActions comment(String url, User user, String content) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + content + "\"}");
        if (user != null) {
            request.header("Authorization", bearer(user));
        }
        return mvc.perform(request);
    }

    private User user(String email, MemberType type, Role role) {
        return User.builder().email(email).name("테스트").provider(AuthProvider.LOCAL)
                .memberType(type).role(role).build();
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
