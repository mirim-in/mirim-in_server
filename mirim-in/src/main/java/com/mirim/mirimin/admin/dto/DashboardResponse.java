package com.mirim.mirimin.admin.dto;

/**
 * 관리자 대시보드.
 * acceptRate: 등록된 질문 중 채택 완료된 질문 비율 (%, 반올림)
 */
public record DashboardResponse(
        long totalUsers,
        long verifiedUsers,
        long questionCount,
        long answerCount,
        int acceptRate,
        long pendingReports,
        long pendingVerifications
) {
}
