package com.mirim.mirimin.global.exception;

import lombok.Getter;

/** 서비스에서 의도적으로 던지는 예외. GlobalExceptionHandler가 ErrorResponse로 바꾼다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 기본 메시지 대신 상황에 맞는 메시지를 내려줄 때 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
