package com.mirim.mirimin.answer.dto;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.answer.entity.VoteType;
import com.mirim.mirimin.user.dto.AuthorResponse;

import java.time.LocalDateTime;

/**
 * myVote: 로그인한 사용자가 누른 추천(LIKE)/비추천(DISLIKE). 누르지 않았거나 비로그인이면 null
 * mine: 로그인한 사용자가 작성자인지 (수정·삭제 버튼 표시)
 */
public record AnswerResponse(
        Long id,
        Long questionId,
        String content,
        AuthorResponse author,
        boolean accepted,
        LocalDateTime acceptedAt,
        int likeCount,
        int dislikeCount,
        VoteType myVote,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AnswerResponse of(Answer answer, VoteType myVote, boolean mine) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getContent(),
                AuthorResponse.of(answer.getAuthor()),
                answer.isAccepted(),
                answer.getAcceptedAt(),
                answer.getLikeCount(),
                answer.getDislikeCount(),
                myVote,
                mine,
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}
