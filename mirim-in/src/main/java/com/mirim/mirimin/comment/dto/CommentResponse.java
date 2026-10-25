package com.mirim.mirimin.comment.dto;

import com.mirim.mirimin.comment.entity.Comment;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.user.dto.AuthorResponse;

import java.time.LocalDateTime;

/**
 * anonymous: 익명 질문의 작성자가 그 질문(또는 그 질문의 답변)에 단 댓글. author는 null
 * mine: 로그인한 사용자가 작성자인지
 */
public record CommentResponse(
        Long id,
        String content,
        boolean anonymous,
        AuthorResponse author,
        boolean mine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CommentResponse of(Comment comment, Long viewerId) {
        Question question = comment.getQuestion();
        // 익명 질문자가 댓글로 신원이 드러나지 않게 한다
        boolean anonymous = question.isAnonymous()
                && question.getAuthor().getId().equals(comment.getAuthor().getId());
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                anonymous,
                anonymous ? null : AuthorResponse.of(comment.getAuthor()),
                viewerId != null && comment.getAuthor().getId().equals(viewerId),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
