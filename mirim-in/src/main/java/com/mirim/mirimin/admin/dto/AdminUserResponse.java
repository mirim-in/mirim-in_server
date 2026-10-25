package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.Role;
import com.mirim.mirimin.user.entity.User;

import java.time.LocalDateTime;

/**
 * 관리자 회원 목록 항목.
 * suspended: 지금 정지 중인지. suspendedUntil이 null이면 영구 정지
 * reportCount: 이 회원의 글이 받은 신고 수 (반복 신고 확인용)
 */
public record AdminUserResponse(
        Long id,
        String name,
        String email,
        MemberType memberType,
        String memberTypeLabel,
        Role role,
        boolean suspended,
        LocalDateTime suspendedUntil,
        int point,
        long reportCount,
        LocalDateTime createdAt
) {

    public static AdminUserResponse of(User user, boolean suspended, long reportCount) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMemberType(),
                user.getMemberType().getLabel(),
                user.getRole(),
                suspended,
                suspended ? user.getSuspendedUntil() : null,
                user.getPoint(),
                reportCount,
                user.getCreatedAt()
        );
    }
}
