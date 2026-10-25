package com.mirim.mirimin.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 학생 이메일 첫 글자(학과 코드)와 학과 매핑.
 * 새 학과가 생기면 상수만 추가하면 된다.
 */
@Getter
@RequiredArgsConstructor
public enum Department {
    SOFTWARE('s', "소프트웨어과"),
    DESIGN('d', "디자인과");

    private final char code;
    private final String label;

    public static Optional<Department> fromCode(char code) {
        char lower = Character.toLowerCase(code);
        return Arrays.stream(values())
                .filter(d -> d.code == lower)
                .findFirst();
    }
}
