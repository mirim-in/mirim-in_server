package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.answer.entity.Answer;

import java.time.LocalDateTime;

/** 프로필의 답변 목록, 대표 답변 */
public record UserAnswerResponse(
        Long answerId,
        Long questionId,
        String questionTitle,
        String preview,
        boolean accepted,
        int likeCount,
        LocalDateTime createdAt
) {

    private static final int PREVIEW_LENGTH = 100;

    public static UserAnswerResponse of(Answer answer) {
        String flat = answer.getContent().replaceAll("\\s+", " ").strip();
        return new UserAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getQuestion().getTitle(),
                flat.length() <= PREVIEW_LENGTH ? flat : flat.substring(0, PREVIEW_LENGTH) + "...",
                answer.isAccepted(),
                answer.getLikeCount(),
                answer.getCreatedAt()
        );
    }
}
