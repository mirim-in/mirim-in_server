package com.mirim.mirimin.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    SLANDER("비방"),
    PROFANITY("욕설"),
    RUDE("불친절"),
    OBSCENE("불건전"),
    ADVERTISEMENT("홍보"),
    ETC("기타");

    private final String label;
}
