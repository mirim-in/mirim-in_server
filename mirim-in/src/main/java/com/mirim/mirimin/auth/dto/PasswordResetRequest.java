package com.mirim.mirimin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** token: 재설정 메일 링크(/reset-password?token=...)에 들어 있는 값 */
public record PasswordResetRequest(
        @NotBlank(message = "재설정 토큰이 필요해요.")
        String token,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
        String newPassword
) {
}
