package com.mirim.mirimin.point.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointReason {
    QUESTION_CREATED("질문 작성"),
    ANSWER_CREATED("답변 작성"),
    ANSWER_ACCEPTED("답변 채택"),
    QUESTION_DELETED("질문 삭제로 회수"),
    ANSWER_DELETED("답변 삭제로 회수"),  // 채택 포인트 포함
    ADMIN_ADJUSTED("관리자 조정");

    private final String label;
}
