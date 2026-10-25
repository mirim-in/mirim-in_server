package com.mirim.mirimin.notification.entity;

public enum NotificationType {
    ANSWER_CREATED,   // 내 질문(또는 "저도 궁금해요" 누른 질문)에 새 답변
    ANSWER_ACCEPTED,  // 내 답변 채택
    COMMENT_CREATED,  // 내 질문/답변에 새 댓글
    CATEGORY_MATCHED, // 답변 가능 분야와 일치하는 새 질문
    NOTICE            // 운영 공지
}
