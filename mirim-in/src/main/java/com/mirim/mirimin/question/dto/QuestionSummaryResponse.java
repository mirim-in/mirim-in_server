package com.mirim.mirimin.question.dto;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionImage;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.user.dto.AuthorResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 목록 카드. anonymous면 author는 null.
 * thumbnailUrl/imageCount: 카드 오른쪽 썸네일과 "3+" 표시용
 */
public record QuestionSummaryResponse(
        Long id,
        String title,
        String preview,
        QuestionStatus status,
        boolean anonymous,
        AuthorResponse author,
        List<CategoryResponse> categories,
        String thumbnailUrl,
        int imageCount,
        int viewCount,
        int answerCount,
        int curiosityCount,
        LocalDateTime publishedAt
) {

    private static final int PREVIEW_LENGTH = 120;

    public static QuestionSummaryResponse of(Question q) {
        List<QuestionImage> images = q.getImages();
        return new QuestionSummaryResponse(
                q.getId(),
                q.getTitle(),
                preview(q.getContent()),
                q.getStatus(),
                q.isAnonymous(),
                q.isAnonymous() ? null : AuthorResponse.of(q.getAuthor()),
                q.getCategories().stream().map(qc -> CategoryResponse.of(qc.getCategory())).toList(),
                images.isEmpty() ? null : images.get(0).getUrl(),
                images.size(),
                q.getViewCount(),
                q.getAnswerCount(),
                q.getCuriosityCount(),
                q.getPublishedAt()
        );
    }

    private static String preview(String content) {
        String flat = content.replaceAll("\\s+", " ").strip();
        return flat.length() <= PREVIEW_LENGTH ? flat : flat.substring(0, PREVIEW_LENGTH) + "...";
    }
}
