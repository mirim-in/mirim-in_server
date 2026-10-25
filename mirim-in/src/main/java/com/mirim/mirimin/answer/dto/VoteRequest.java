package com.mirim.mirimin.answer.dto;

import com.mirim.mirimin.answer.entity.VoteType;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull(message = "LIKE 또는 DISLIKE를 선택해 주세요.")
        VoteType type
) {
}
