package com.mirim.mirimin.question.dto;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionImage;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.user.dto.AuthorResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 질문 상세. anonymous면 author는 null.
 * mine: 로그인한 사용자가 작성자인지 (수정·삭제·채택 버튼 표시)
 * curious, bookmarked: 로그인한 사용자가 "저도 궁금해요"/즐겨찾기를 눌렀는지. 비로그인이면 false
 */
public record QuestionDetailResponse(
        Long id,
        String title,
        String content,
        QuestionStatus status,
        boolean anonymous,
        AuthorResponse author,
        List<CategoryResponse> categories,
        List<String> imageUrls,
        int viewCount,
        int answerCount,
        int curiosityCount,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        boolean mine,
        boolean curious,
        boolean bookmarked
) {

    public static QuestionDetailResponse of(Question q, int viewCount, boolean mine,
                                            boolean curious, boolean bookmarked) {
        return new QuestionDetailResponse(
                q.getId(),
                q.getTitle(),
                q.getContent(),
                q.getStatus(),
                q.isAnonymous(),
                q.isAnonymous() ? null : AuthorResponse.of(q.getAuthor()),
                QuestionSummaryResponse.visibleCategories(q),
                q.getImages().stream().map(QuestionImage::getUrl).toList(),
                viewCount,
                q.getAnswerCount(),
                q.getCuriosityCount(),
                q.getPublishedAt(),
                q.getUpdatedAt(),
                mine,
                curious,
                bookmarked
        );
    }
}
