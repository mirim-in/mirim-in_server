package com.mirim.mirimin.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** sortOrder: 공식 카테고리 칩 노출 순서 (작을수록 앞). 비우면 맨 뒤 */
public record CategoryRequest(
        @NotBlank(message = "카테고리 이름을 입력해 주세요.")
        @Size(max = 30, message = "카테고리 이름은 30자 이하로 입력해 주세요.")
        String name,

        @Min(value = 0, message = "순서는 0 이상이어야 해요.")
        Integer sortOrder
) {
}
