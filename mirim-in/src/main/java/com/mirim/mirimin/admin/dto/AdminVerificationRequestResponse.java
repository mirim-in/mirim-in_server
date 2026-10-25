package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.MemberVerificationRequest;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;

import java.time.LocalDateTime;

/** 관리자 > 미림인 승인 목록 항목. currentMemberType은 신청자의 지금 유형 (보통 GENERAL) */
public record AdminVerificationRequestResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        MemberType currentMemberType,
        MemberType requestedType,
        String requestedTypeLabel,
        String message,
        VerificationRequestStatus status,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {

    public static AdminVerificationRequestResponse of(MemberVerificationRequest request) {
        User user = request.getUser();
        return new AdminVerificationRequestResponse(
                request.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMemberType(),
                request.getRequestedType(),
                request.getRequestedType().getLabel(),
                request.getMessage(),
                request.getStatus(),
                request.getRejectReason(),
                request.getCreatedAt(),
                request.getReviewedAt()
        );
    }
}
