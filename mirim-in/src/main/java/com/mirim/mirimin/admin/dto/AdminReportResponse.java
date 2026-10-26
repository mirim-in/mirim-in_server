package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.entity.ReportTargetType;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자 신고 목록 항목.
 * questionId: 신고된 글이 속한 질문 (답변·댓글이면 부모 질문). 화면 이동용
 * contentSnapshot: 신고 당시 내용
 * targetDeleted: 신고된 글이 이미 삭제됐는지
 */
public record AdminReportResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        Long questionId,
        boolean targetDeleted,
        List<ReportReason> reasons,
        List<String> reasonLabels,
        String detail,
        String contentSnapshot,
        ReportStatus status,
        Long reporterId,
        String reporterName,
        Long targetUserId,
        String targetUserName,
        String targetUserEmail,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {

    public static AdminReportResponse of(Report report, Long questionId, boolean targetDeleted) {
        List<ReportReason> reasons = report.getReasons().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        return new AdminReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                questionId,
                targetDeleted,
                reasons,
                reasons.stream().map(ReportReason::getLabel).toList(),
                report.getDetail(),
                report.getContentSnapshot(),
                report.getStatus(),
                report.getReporter().getId(),
                report.getReporter().getName(),
                report.getTargetUser().getId(),
                report.getTargetUser().getName(),
                report.getTargetUser().getEmail(),
                report.getCreatedAt(),
                report.getProcessedAt()
        );
    }
}
