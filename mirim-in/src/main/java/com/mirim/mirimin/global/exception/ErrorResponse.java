package com.mirim.mirimin.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 모든 에러 응답의 형식.
 * { "code": "C001", "message": "입력값이 올바르지 않아요.", "errors": [{ "field": "email", "reason": "..." }] }
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldError> errors) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errors);
    }
}
