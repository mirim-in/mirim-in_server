package com.mirim.mirimin.category.dto;

import com.mirim.mirimin.category.entity.Category;

/** official=false 는 사용자가 질문 작성 중 직접 추가한 카테고리 */
public record CategoryResponse(Long id, String name, boolean official) {

    public static CategoryResponse of(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.isOfficial());
    }
}
