package com.mirim.mirimin.answer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerRequest(
        @NotBlank(message = "답변 내용을 입력해 주세요.")
        @Size(max = 10000, message = "답변은 10,000자 이하로 입력해 주세요.")
        String content
) {
}
