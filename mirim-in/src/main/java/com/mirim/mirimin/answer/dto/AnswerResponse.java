package com.mirim.mirimin.answer.dto;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.answer.entity.VoteType;
import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.user.dto.AuthorResponse;
import com.mirim.mirimin.user.service.UserTagReader;

import java.time.LocalDateTime;
import java.util.List;

/**
 * myVote: 로그인한 사용자가 누른 추천(LIKE)/비추천(DISLIKE). 누르지 않았거나 비로그인이면 null
 * mine: 로그인한 사용자가 작성자인지 (수정·삭제 버튼 표시)
 * authorInterests, authorTechStacks: 답변자의 관심 분야 이름, 기술 스택 (요구사항 AN-004)
 */
public record AnswerResponse(
        Long id,
        Long questionId,
        String content,
        AuthorResponse author,
        List<String> authorInterests,
        List<String> authorTechStacks,
        boolean accepted,
        LocalDateTime acceptedAt,
        int likeCount,
        int dislikeCount,
        VoteType myVote,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AnswerResponse of(Answer answer, VoteType myVote, boolean mine, UserTagReader.UserTags authorTags) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getContent(),
                AuthorResponse.of(answer.getAuthor()),
                authorTags.interests().stream().map(CategoryResponse::name).toList(),
                authorTags.techStacks(),
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
