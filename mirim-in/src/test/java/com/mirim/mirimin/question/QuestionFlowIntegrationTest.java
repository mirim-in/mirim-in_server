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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 DB(.env의 DB_*)에 붙어 질문 API를 확인한다. 테스트가 끝나면 롤백된다. */
@SpringBootTest
@Transactional
class QuestionFlowIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    JwtProvider jwtProvider;

    MockMvc mvc;
    User author;
    User other;
    Long dormitoryId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        author = userRepository.save(user("q-author@example.com", MemberType.GENERAL));
        other = userRepository.save(user("q-other@e-mirim.hs.kr", MemberType.TEACHER));
        dormitoryId = categoryRepository.findByName("기숙사").orElseThrow().getId();
    }

    @Test
    void 질문을_작성하고_목록_검색_상세_수정_삭제한다() throws Exception {
        String body = """
                {"title":"기숙사에서 라면 먹어도 되나요?","content":"야식이 궁금해요",
                 "categoryIds":[%d],"newCategories":["야식테스트키워드"],
                 "imageUrls":["https://res.cloudinary.com/demo/a.png"],"anonymous":true,"draft":false}
                """.formatted(dormitoryId);
        String id = mvc.perform(post("/api/questions").header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 비로그인 상세: 익명이라 작성자 없음, 조회수 1
        mvc.perform(get("/api/questions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymous").value(true))
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://res.cloudinary.com/demo/a.png"))
                .andExpect(jsonPath("$.viewCount").value(1))
                .andExpect(jsonPath("$.mine").value(false));
        // 같은 사람이 다시 보면 조회수 그대로
        mvc.perform(get("/api/questions/" + id)).andExpect(jsonPath("$.viewCount").value(1));
        // 작성자가 보면 mine=true, 조회수 2
        mvc.perform(get("/api/questions/" + id).header("Authorization", bearer(author)))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.viewCount").value(2));

        // 키워드(카테고리 이름) 검색, 카테고리 필터
        mvc.perform(get("/api/questions").param("keyword", "야식테스트"))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(id))));
        mvc.perform(get("/api/questions").param("categoryId", String.valueOf(dormitoryId)).param("size", "50"))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(id))));
        mvc.perform(get("/api/questions/popular").param("period", "WEEK").param("size", "50"))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(id))));

        // 다른 사람은 수정·삭제 불가
        String update = """
                {"title":"수정된 제목","content":"수정된 내용","categoryIds":[%d],"anonymous":false,"draft":false}
                """.formatted(dormitoryId);
        mvc.perform(put("/api/questions/" + id).header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Q002"));

        mvc.perform(put("/api/questions/" + id).header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + id))
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.author.name").value("테스트"))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.imageUrls.length()").value(0));

        mvc.perform(delete("/api/questions/" + id).header("Authorization", bearer(author)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Q001"));
    }

    @Test
    void 임시_보관_질문은_목록에_안_나오고_작성자만_본다() throws Exception {
        String id = mvc.perform(post("/api/questions").header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"임시 질문\",\"content\":\"작성 중\",\"draft\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/questions").param("keyword", "임시 질문"))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(Integer.valueOf(id)))));
        mvc.perform(get("/api/questions/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/questions/drafts").header("Authorization", bearer(author)))
                .andExpect(jsonPath("$[0].id").value(Integer.valueOf(id)))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
        mvc.perform(get("/api/questions/drafts")).andExpect(status().isUnauthorized());

        // 등록하면 목록에 나온다
        mvc.perform(put("/api/questions/" + id).header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"임시 질문\",\"content\":\"완성\",\"draft\":false}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions").param("keyword", "임시 질문"))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(Integer.valueOf(id))));
    }

    @Test
    void 정지된_사용자는_질문을_쓸_수_없고_입력값_검증이_된다() throws Exception {
        author.suspend(LocalDateTime.now().plusDays(3));
        mvc.perform(post("/api/questions").header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("U004"));

        mvc.perform(post("/api/questions").header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"내용\",\"imageUrls\":[\"https://evil.com/a.png\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        mvc.perform(post("/api/questions").header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\",\"newCategories\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("G002"));

        mvc.perform(post("/api/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 공식_카테고리_목록을_조회한다() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("기숙사"))
                .andExpect(jsonPath("$[0].official").value(true));
    }

    private User user(String email, MemberType type) {
        return User.builder().email(email).name("테스트").provider(AuthProvider.LOCAL).memberType(type).build();
    }

    private String bearer(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
