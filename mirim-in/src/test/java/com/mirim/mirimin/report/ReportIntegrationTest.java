package com.mirim.mirimin.report;

import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.repository.ReportRepository;
import com.mirim.mirimin.user.entity.AuthProvider;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 신고 접수를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class ReportIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ReportRepository reportRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User asker;
    User student;
    User reporter;
    String questionId;
    String answerId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        asker = save("r-asker@example.com", MemberType.GENERAL);
        student = save("s9905@e-mirim.hs.kr", MemberType.STUDENT);
        reporter = save("r-reporter@example.com", MemberType.GENERAL);

        questionId = postJson("/api/questions", asker, "{\"title\":\"질문\",\"content\":\"내용\"}")
                .andReturn().getResponse().getContentAsString();
        answerId = postJson("/api/questions/" + questionId + "/answers", student,
                "{\"content\":\"그 정도는 검색하면 나오는데 왜 여기에 물어보세요\"}")
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 답변을_신고하면_신고_당시_내용이_저장되고_같은_글은_한_번만_신고된다() throws Exception {
        String reportId = report(reporter, "ANSWER", answerId, "[\"SLANDER\",\"RUDE\"]", null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 신고 후 답변을 수정해도 신고 당시 내용이 남는다
        mvc.perform(put("/api/answers/" + answerId).header("Authorization", bearer(student))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정한 답변\"}"));
        Report saved = reportRepository.findById(Long.valueOf(reportId)).orElseThrow();
        assertThat(saved.getContentSnapshot()).isEqualTo("그 정도는 검색하면 나오는데 왜 여기에 물어보세요");
        assertThat(saved.getTargetUser().getId()).isEqualTo(student.getId());
        assertThat(saved.getReasons()).containsExactlyInAnyOrder(ReportReason.SLANDER, ReportReason.RUDE);
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);

        report(reporter, "ANSWER", answerId, "[\"PROFANITY\"]", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("P002"));
    }

    @Test
    void 본인_글_신고와_잘못된_요청은_거부된다() throws Exception {
        report(student, "ANSWER", answerId, "[\"SLANDER\"]", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P001"));
        report(reporter, "QUESTION", questionId, "[\"ETC\"]", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P003"));
        report(reporter, "QUESTION", questionId, "[]", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reasons"));
        report(reporter, "COMMENT", "999999999", "[\"SLANDER\"]", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("M001"));

        report(reporter, "QUESTION", questionId, "[\"ETC\"]", "\"광고성 글 같아요\"")
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"QUESTION\",\"targetId\":1,\"reasons\":[\"SLANDER\"]}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions report(User user, String type, String targetId, String reasons, String detail)
            throws Exception {
        String body = "{\"targetType\":\"%s\",\"targetId\":%s,\"reasons\":%s,\"detail\":%s}"
                .formatted(type, targetId, reasons, detail == null ? "null" : detail);
        return postJson("/api/reports", user, body);
    }

    private ResultActions postJson(String url, User user, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private User save(String email, MemberType type) {
        return userRepository.save(User.builder().email(email).name("테스트").provider(AuthProvider.LOCAL)
                .memberType(type).build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
