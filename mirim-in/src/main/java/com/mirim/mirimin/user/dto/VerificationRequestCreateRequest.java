package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.user.entity.MemberType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** requestedType: GRADUATE(졸업생), TEACHER(교직원), STUDENT(재학생) */
public record VerificationRequestCreateRequest(
        @NotNull(message = "인증받을 유형을 선택해 주세요.")
        MemberType requestedType,

        @Size(max = 500, message = "메시지는 500자 이하로 입력해 주세요.")
        String message
) {
}
