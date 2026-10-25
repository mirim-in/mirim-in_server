package com.mirim.mirimin.notification.dto;

import com.mirim.mirimin.notification.entity.Notification;
import com.mirim.mirimin.notification.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * 사이드바 알림 항목. 예) message "강별님이 답변을 남겼어요.", questionTitle "기숙사에서 라면 먹어도 되나요?"
 * 클릭하면 questionId(공지는 noticeId)로 이동한다. 삭제된 질문이면 questionId/questionTitle이 null
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        Long questionId,
        String questionTitle,
        Long noticeId,
        boolean read,
        LocalDateTime createdAt
) {

    public static NotificationResponse of(Notification n) {
        boolean questionVisible = n.getQuestion() != null && !n.getQuestion().isDeleted();
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                questionVisible ? n.getQuestion().getId() : null,
                questionVisible ? n.getQuestion().getTitle() : null,
                n.getNotice() != null ? n.getNotice().getId() : null,
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
