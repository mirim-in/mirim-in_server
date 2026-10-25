package com.mirim.mirimin.point.dto;

import com.mirim.mirimin.point.entity.PointHistory;
import com.mirim.mirimin.point.entity.PointReason;

import java.time.LocalDateTime;

/** amount가 음수면 차감. referenceId: 관련 질문 또는 답변 id (reason으로 구분) */
public record PointHistoryResponse(
        Long id,
        int amount,
        PointReason reason,
        Long referenceId,
        String memo,
        LocalDateTime createdAt
) {

    public static PointHistoryResponse of(PointHistory history) {
        return new PointHistoryResponse(history.getId(), history.getAmount(), history.getReason(),
                history.getReferenceId(), history.getMemo(), history.getCreatedAt());
    }
}
