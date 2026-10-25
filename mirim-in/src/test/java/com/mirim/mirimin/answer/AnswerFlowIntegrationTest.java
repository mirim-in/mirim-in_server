package com.mirim.mirimin.answer;

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

/** 실제 DB(.env의 DB_*)에 붙어 답변 API를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class AnswerFlowIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User questioner;   // 일반 사용자 (중학생)
    User student;      // 재학생
    User teacher;      // 교직원
    User admin;
    String questionId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        questioner = userRepository.save(user("a-questioner@example.com", MemberType.GENERAL, Role.USER));
        student = userRepository.save(user("s2598@e-mirim.hs.kr", MemberType.STUDENT, Role.USER));
        teacher = userRepository.save(user("a-teacher@e-mirim.hs.kr", MemberType.TEACHER, Role.USER));
        admin = userRepository.save(user("a-admin@example.com", MemberType.GENERAL, Role.ADMIN));

        questionId = mvc.perform(post("/api/questions").header("Authorization", bearer(questioner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"면접 질문이 궁금해요\",\"content\":\"어떤 걸 물어보나요?\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 구성원만_답변하고_추천순으로_정렬되며_질문자가_채택한다() throws Exception {
        // 일반 사용자는 답변 불가
        writeAnswer(questioner, "저도 궁금해요")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("R003"));

        String studentAnswer = writeAnswer(student, "자기소개를 물어봐요").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String teacherAnswer = writeAnswer(teacher, "전공 관련 질문을 해요").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/questions/" + questionId))
                .andExpect(jsonPath("$.answerCount").value(2));

        // 추천: 교직원 답변에 2개 → 먼저 썼어도 학생 답변이 아래로
        vote(questioner, teacherAnswer, "LIKE").andExpect(status().isNoContent());
        vote(student, teacherAnswer, "LIKE").andExpect(status().isNoContent());
        vote(teacher, teacherAnswer, "LIKE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("R007"));

        mvc.perform(get("/api/questions/" + questionId + "/answers").header("Authorization", bearer(questioner)))
                .andExpect(jsonPath("$[0].id").value(Integer.valueOf(teacherAnswer)))
                .andExpect(jsonPath("$[0].likeCount").value(2))
                .andExpect(jsonPath("$[0].myVote").value("LIKE"))
                .andExpect(jsonPath("$[0].author.memberTypeLabel").value("교직원"))
                .andExpect(jsonPath("$[0].author.verified").value(true))
                .andExpect(jsonPath("$[1].myVote").doesNotExist());

        // 추천 → 비추천으로 변경, 취소
        vote(questioner, teacherAnswer, "DISLIKE").andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId + "/answers"))
                .andExpect(jsonPath("$[0].likeCount").value(1))
                .andExpect(jsonPath("$[0].dislikeCount").value(1));
        mvc.perform(delete("/api/answers/" + teacherAnswer + "/vote").header("Authorization", bearer(questioner)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId + "/answers"))
                .andExpect(jsonPath("$[0].dislikeCount").value(0));

        // 질문자가 아니면 채택 불가
        accept(student, teacherAnswer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("R005"));

        // 학생 답변 채택 → 맨 위, 질문은 채택 완료
        accept(questioner, studentAnswer).andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId + "/answers"))
                .andExpect(jsonPath("$[0].id").value(Integer.valueOf(studentAnswer)))
                .andExpect(jsonPath("$[0].accepted").value(true));
        mvc.perform(get("/api/questions/" + questionId))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        accept(questioner, teacherAnswer)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("R004"));

        // 채택된 답변은 작성자가 삭제 불가, 관리자는 가능 → 질문 다시 답변 대기
        mvc.perform(delete("/api/answers/" + studentAnswer).header("Authorization", bearer(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("R008"));
        mvc.perform(delete("/api/answers/" + studentAnswer).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.answerCount").value(1));
    }

    @Test
    void 본인_답변은_채택할_수_없고_작성자만_수정한다() throws Exception {
        // 재학생이 직접 질문하고 직접 답변
        String ownQuestion = mvc.perform(post("/api/questions").header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"자문자답\",\"content\":\"내용\"}"))
                .andReturn().getResponse().getContentAsString();
        String ownAnswer = mvc.perform(post("/api/questions/" + ownQuestion + "/answers")
                        .header("Authorization", bearer(student)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"제가 답할게요\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        accept(student, ownAnswer)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("R006"));

        mvc.perform(put("/api/answers/" + ownAnswer).header("Authorization", bearer(teacher))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"남의 답변 수정\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("R002"));
        mvc.perform(put("/api/answers/" + ownAnswer).header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정했어요\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + ownQuestion + "/answers").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$[0].content").value("수정했어요"))
                .andExpect(jsonPath("$[0].mine").value(true));

        mvc.perform(post("/api/questions/" + ownQuestion + "/answers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"비로그인\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions writeAnswer(User user, String content) throws Exception {
        return mvc.perform(post("/api/questions/" + questionId + "/answers").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + content + "\"}"));
    }

    private ResultActions vote(User user, String answerId, String type) throws Exception {
        return mvc.perform(put("/api/answers/" + answerId + "/vote").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"" + type + "\"}"));
    }

    private ResultActions accept(User user, String answerId) throws Exception {
        return mvc.perform(post("/api/answers/" + answerId + "/accept").header("Authorization", bearer(user)));
    }

    private User user(String email, MemberType type, Role role) {
        return User.builder().email(email).name("테스트").provider(AuthProvider.LOCAL)
                .memberType(type).role(role).build();
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
