package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.user.dto.AuthorResponse;
import com.mirim.mirimin.user.dto.UserAnswerResponse;

/** 관리자 답변 목록 */
public record AdminAnswerResponse(
        UserAnswerResponse answer,
        AuthorResponse author
) {

    public static AdminAnswerResponse of(Answer a) {
        return new AdminAnswerResponse(UserAnswerResponse.of(a), AuthorResponse.of(a.getAuthor()));
    }
}
