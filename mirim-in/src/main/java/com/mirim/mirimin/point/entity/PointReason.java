package com.mirim.mirimin.point.entity;

public enum PointReason {
    QUESTION_CREATED,
    ANSWER_CREATED,
    ANSWER_ACCEPTED,
    QUESTION_DELETED,  // 회수
    ANSWER_DELETED,    // 회수 (채택 포인트 포함)
    ADMIN_ADJUSTED
}
