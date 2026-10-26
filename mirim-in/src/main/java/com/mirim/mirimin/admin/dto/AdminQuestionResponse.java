package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.user.dto.AuthorResponse;

/** 관리자 질문 목록. 익명 질문이어도 실제 작성자(realAuthor)를 보여준다. */
public record AdminQuestionResponse(
        QuestionSummaryResponse question,
        AuthorResponse realAuthor
) {

    public static AdminQuestionResponse of(Question q) {
        return new AdminQuestionResponse(QuestionSummaryResponse.of(q), AuthorResponse.of(q.getAuthor()));
    }
}
