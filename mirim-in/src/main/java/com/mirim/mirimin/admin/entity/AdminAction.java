package com.mirim.mirimin.admin.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminAction {
    APPROVE_MEMBER("미림인 승인"),
    REJECT_MEMBER("미림인 반려"),
    SUSPEND_USER("회원 정지"),
    UNSUSPEND_USER("정지 해제"),
    FORCE_WITHDRAW("강제 탈퇴"),
    DELETE_QUESTION("질문 삭제"),
    DELETE_ANSWER("답변 삭제"),
    DELETE_COMMENT("댓글 삭제"),
    CREATE_CATEGORY("카테고리 추가"),
    UPDATE_CATEGORY("카테고리 수정"),
    DELETE_CATEGORY("카테고리 삭제"),
    PROCESS_REPORT("신고 처리"),
    CREATE_NOTICE("공지 작성"),
    UPDATE_NOTICE("공지 수정"),
    DELETE_NOTICE("공지 삭제"),
    CREATE_FAQ("FAQ 등록"),
    UPDATE_FAQ("FAQ 수정"),
    DELETE_FAQ("FAQ 삭제"),
    UPDATE_POINT_POLICY("포인트 정책 변경"),
    ADJUST_POINT("포인트 수동 조정");

    private final String label;
}
