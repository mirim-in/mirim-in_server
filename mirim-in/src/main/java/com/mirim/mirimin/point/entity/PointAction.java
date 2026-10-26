package com.mirim.mirimin.point.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 관리자가 지급량을 조정할 수 있는 행동. defaultAmount는 정책이 없을 때 쓰는 기본값. */
@Getter
@RequiredArgsConstructor
public enum PointAction {
    QUESTION("질문 작성", 5),
    ANSWER("답변 작성", 10),
    ACCEPTED("답변 채택", 30);

    private final String label;
    private final int defaultAmount;
}
