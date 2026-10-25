package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.MemberVerificationRequest;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;

import java.time.LocalDateTime;

/** rejectReason: 반려된 경우 관리자가 남긴 사유 */
public record VerificationRequestResponse(
        Long id,
        MemberType requestedType,
        String requestedTypeLabel,
        String message,
        VerificationRequestStatus status,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {

    public static VerificationRequestResponse of(MemberVerificationRequest request) {
        return new VerificationRequestResponse(
                request.getId(),
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
