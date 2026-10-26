package com.mirim.mirimin.faq.dto;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.faq.entity.Faq;

/** category: 분류 없음이거나 카테고리가 삭제됐으면 null */
public record FaqResponse(
        Long id,
        CategoryResponse category,
        String question,
        String answer,
        int sortOrder
) {

    public static FaqResponse of(Faq faq) {
        boolean hasCategory = faq.getCategory() != null && !faq.getCategory().isDeleted();
        return new FaqResponse(
                faq.getId(),
                hasCategory ? CategoryResponse.of(faq.getCategory()) : null,
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getSortOrder()
        );
    }
}
