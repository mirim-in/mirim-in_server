package com.mirim.mirimin.auth.dto;

/** 가입, 비밀번호 재설정에서 같은 규칙을 쓴다. */
final class PasswordRules {

    static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    static final String MESSAGE = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 해요.";

    private PasswordRules() {
    }
}
