package com.mirim.mirimin.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 응답의 code는 프론트에서 분기용으로 쓴다. 한 번 정한 code는 바꾸지 않는다.
 * 도메인 에러는 접두어로 구분한다. (C: 공통, A: 인증, U: 회원 ...)
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 공통 =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않아요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "로그인이 필요해요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "권한이 없어요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 대상을 찾을 수 없어요."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005", "지원하지 않는 요청 방식이에요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C006", "서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요."),

    // ===== 인증 =====
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "이메일 또는 비밀번호가 올바르지 않아요."),
    // 프론트는 이 코드를 받으면 Refresh Token으로 재발급을 시도한다
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "인증 정보가 만료되었거나 올바르지 않아요."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "A003", "이메일 인증을 먼저 완료해 주세요."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "A004", "인증 코드가 올바르지 않거나 만료되었어요."),
    VERIFICATION_LOCKED(HttpStatus.BAD_REQUEST, "A005", "인증 시도 횟수를 초과했어요. 코드를 다시 받아 주세요."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "A006", "잠시 후 다시 요청해 주세요."),
    SOCIAL_ACCOUNT(HttpStatus.BAD_REQUEST, "A007", "Google로 가입한 계정이에요. Google 로그인을 이용해 주세요."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A008", "메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요."),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "A009", "비밀번호 재설정 링크가 만료되었거나 이미 사용되었어요."),

    // ===== 회원 =====
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 가입된 이메일이에요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "회원을 찾을 수 없어요."),
    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "U003", "탈퇴한 계정이에요."),
    WRITE_RESTRICTED(HttpStatus.FORBIDDEN, "U004", "활동이 제한된 계정이라 글을 쓸 수 없어요."),
    ANSWERABLE_REQUIRES_VERIFIED(HttpStatus.FORBIDDEN, "U005", "학교 구성원 인증을 받은 사용자만 답변 가능 분야를 설정할 수 있어요."),

    // ===== 이미지 =====
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "I001", "jpg, png, gif, webp 이미지만 올릴 수 있어요."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I002", "이미지를 올리지 못했어요. 잠시 후 다시 시도해 주세요."),
    IMAGE_UPLOAD_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "I003", "이미지 업로드가 설정되지 않았어요. 관리자에게 문의해 주세요."),

    // ===== 카테고리 =====
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "G001", "선택할 수 없는 카테고리예요."),
    TOO_MANY_CATEGORIES(HttpStatus.BAD_REQUEST, "G002", "카테고리는 5개까지 선택할 수 있어요."),

    // ===== 질문 =====
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "질문을 찾을 수 없어요."),
    NOT_QUESTION_AUTHOR(HttpStatus.FORBIDDEN, "Q002", "본인이 작성한 질문만 수정·삭제할 수 있어요."),
    CANNOT_CURIOUS_OWN_QUESTION(HttpStatus.BAD_REQUEST, "Q003", "본인 질문에는 '저도 궁금해요'를 누를 수 없어요."),

    // ===== 댓글 =====
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "댓글을 찾을 수 없어요."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "M002", "본인이 작성한 댓글만 수정·삭제할 수 있어요."),

    // ===== 답변 =====
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "답변을 찾을 수 없어요."),
    NOT_ANSWER_AUTHOR(HttpStatus.FORBIDDEN, "R002", "본인이 작성한 답변만 수정·삭제할 수 있어요."),
    NOT_VERIFIED_MEMBER(HttpStatus.FORBIDDEN, "R003", "학교 구성원 인증을 받은 사용자만 답변할 수 있어요."),
    ALREADY_ACCEPTED(HttpStatus.CONFLICT, "R004", "이미 채택된 답변이 있어요."),
    NOT_QUESTION_AUTHOR_TO_ACCEPT(HttpStatus.FORBIDDEN, "R005", "질문 작성자만 답변을 채택할 수 있어요."),
    CANNOT_ACCEPT_OWN_ANSWER(HttpStatus.BAD_REQUEST, "R006", "본인 답변은 채택할 수 없어요."),
    CANNOT_VOTE_OWN_ANSWER(HttpStatus.BAD_REQUEST, "R007", "본인 답변에는 추천할 수 없어요."),
    CANNOT_DELETE_ACCEPTED_ANSWER(HttpStatus.BAD_REQUEST, "R008", "채택된 답변은 삭제할 수 없어요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
