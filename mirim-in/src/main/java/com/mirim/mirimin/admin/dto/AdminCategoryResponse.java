package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.category.entity.Category;

import java.time.LocalDateTime;

/** official=false는 사용자가 질문 작성 중 만든 카테고리. questionCount로 정리 대상을 고른다. */
public record AdminCategoryResponse(
        Long id,
        String name,
        boolean official,
        int sortOrder,
        long questionCount,
        String createdByName,
        LocalDateTime createdAt
) {

    public static AdminCategoryResponse of(Category c, long questionCount) {
        return new AdminCategoryResponse(
                c.getId(),
                c.getName(),
                c.isOfficial(),
                c.getSortOrder(),
                questionCount,
                c.getCreatedBy() != null ? c.getCreatedBy().getName() : null,
                c.getCreatedAt()
        );
    }
}
