package com.mirim.mirimin.question;

import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.category.repository.CategoryRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 유사 질문 추천을 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class SimilarQuestionIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User user;
    Long dormitoryId;
    Long interviewId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        user = userRepository.save(User.builder().email("sim-user@example.com").name("테스트")
                .provider(AuthProvider.LOCAL).memberType(MemberType.GENERAL).build());
        dormitoryId = categoryRepository.findByName("기숙사").orElseThrow().getId();
        interviewId = categoryRepository.findByName("면접").orElseThrow().getId();
    }

    @Test
    void 제목_단어와_카테고리가_많이_겹치는_질문을_먼저_추천한다() throws Exception {
        String curfew = create("유사테스트 기숙사 통금 시간이 궁금해요", dormitoryId);
        String ramen = create("유사테스트 기숙사에서 라면 먹어도 되나요", dormitoryId);
        String interview = create("유사테스트 면접 준비 방법", interviewId);

        // 기숙사 + 통금 → 통금 질문이 1위, 라면 질문(기숙사만 겹침)이 2위, 면접 질문은 없음
        mvc.perform(get("/api/questions/similar")
                        .param("title", "기숙사에서 통금은 몇 시인가요?")
                        .param("categoryIds", String.valueOf(dormitoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Integer.valueOf(curfew)))
                .andExpect(jsonPath("$[1].id").value(Integer.valueOf(ramen)))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(Integer.valueOf(interview)))));

        // 제목이 겹치지 않아도 카테고리가 같으면 추천
        mvc.perform(get("/api/questions/similar")
                        .param("title", "자기소개 팁 있을까요")
                        .param("categoryIds", String.valueOf(interviewId)))
                .andExpect(jsonPath("$[0].id").value(Integer.valueOf(interview)));

        // 수정 중인 질문은 제외
        mvc.perform(get("/api/questions/similar")
                        .param("title", "기숙사 통금")
                        .param("excludeId", curfew))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(Integer.valueOf(curfew)))));

        mvc.perform(get("/api/questions/similar").param("title", "a"))
                .andExpect(status().isBadRequest());
    }

    private String create(String title, Long categoryId) throws Exception {
        return mvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"content\":\"내용\",\"categoryIds\":[%d]}".formatted(title, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
