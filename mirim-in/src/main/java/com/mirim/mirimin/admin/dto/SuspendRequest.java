package com.mirim.mirimin.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** days가 null이면 영구 정지. 정지 중에도 로그인은 되고 글쓰기만 막힌다. */
public record SuspendRequest(
        @Min(value = 1, message = "정지 기간은 1일 이상이어야 해요.")
        @Max(value = 365, message = "기간 정지는 365일까지 가능해요. 그 이상은 영구 정지를 선택해 주세요.")
        Integer days,

        @Size(max = 200, message = "사유는 200자 이하로 입력해 주세요.")
        String reason
) {
}
