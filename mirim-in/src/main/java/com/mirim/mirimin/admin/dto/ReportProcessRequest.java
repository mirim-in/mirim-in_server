package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.report.entity.ReportStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 신고 처리. 상태 변경과 함께 조치를 고를 수 있다.
 * status: ON_HOLD(보류) 또는 DONE(처리 완료)
 * suspendDays / permanentSuspend: 신고된 글의 작성자 정지 (둘 다 비우면 정지 안 함)
 * deleteContent: 신고된 글 삭제. 같은 글의 다른 미처리 신고도 처리 완료된다.
 */
public record ReportProcessRequest(
        @NotNull(message = "처리 상태를 선택해 주세요.")
        ReportStatus status,

        @Min(value = 1, message = "정지 기간은 1일 이상이어야 해요.")
        @Max(value = 365, message = "기간 정지는 365일까지 가능해요.")
        Integer suspendDays,

        boolean permanentSuspend,

        boolean deleteContent
) {

    public boolean suspends() {
        return permanentSuspend || suspendDays != null;
    }
}
