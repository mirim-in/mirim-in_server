package com.mirim.mirimin.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointPolicyRequest(
        @NotNull(message = "지급 포인트를 입력해 주세요.")
        @Min(value = 0, message = "지급 포인트는 0 이상이어야 해요.")
        @Max(value = 1000, message = "지급 포인트는 1,000 이하여야 해요.")
        Integer amount
) {
}
