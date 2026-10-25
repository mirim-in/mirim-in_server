package com.mirim.mirimin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 이메일 인증(코드 확인)을 마친 뒤에 호출한다. */
public record SignupRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이하로 입력해 주세요.")
        String name,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않아요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp = PasswordRules.PATTERN, message = PasswordRules.MESSAGE)
        String password
) {
}
