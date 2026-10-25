package com.mirim.mirimin.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** intro: 멘토 카드의 한 줄 소개 (예: "UX/UI 포트폴리오 관련 질문 답변 가능해요.") */
public record MentorUpdateRequest(
        @NotNull(message = "멘토 여부를 선택해 주세요.")
        Boolean enabled,

        @Size(max = 200, message = "소개는 200자 이하로 입력해 주세요.")
        String intro
) {
}
