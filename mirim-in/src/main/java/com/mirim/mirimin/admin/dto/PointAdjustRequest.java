package com.mirim.mirimin.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** amount: 양수면 지급, 음수면 차감. memo는 사용자 포인트 내역에도 보인다. */
public record PointAdjustRequest(
        @NotNull(message = "조정할 포인트를 입력해 주세요.")
        @Min(value = -100000, message = "한 번에 100,000까지 조정할 수 있어요.")
        @Max(value = 100000, message = "한 번에 100,000까지 조정할 수 있어요.")
        Integer amount,

        @NotBlank(message = "조정 사유를 입력해 주세요.")
        @Size(max = 200, message = "사유는 200자 이하로 입력해 주세요.")
        String memo
) {
}
