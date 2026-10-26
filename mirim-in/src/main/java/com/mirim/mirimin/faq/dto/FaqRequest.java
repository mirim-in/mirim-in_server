package com.mirim.mirimin.faq.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** categoryId: 공식 카테고리 (없어도 됨). sortOrder: 작을수록 위, 비우면 맨 아래 */
public record FaqRequest(
        Long categoryId,

        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 200, message = "질문은 200자 이하로 입력해 주세요.")
        String question,

        @NotBlank(message = "답변을 입력해 주세요.")
        @Size(max = 10000, message = "답변은 10,000자 이하로 입력해 주세요.")
        String answer,

        @Min(value = 0, message = "순서는 0 이상이어야 해요.")
        Integer sortOrder
) {
}
