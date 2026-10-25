package com.mirim.mirimin.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberType {
    GENERAL("일반"),
    STUDENT("재학생"),
    GRADUATE("졸업생"),
    TEACHER("교직원");

    private final String label;
}
