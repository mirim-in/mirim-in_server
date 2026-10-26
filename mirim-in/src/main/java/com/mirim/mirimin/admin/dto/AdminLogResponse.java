package com.mirim.mirimin.admin.dto;

import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.entity.AdminLog;

import java.time.LocalDateTime;

/** targetType: USER, QUESTION, ANSWER, COMMENT, REPORT, VERIFICATION_REQUEST, CATEGORY, NOTICE, FAQ, POINT_POLICY */
public record AdminLogResponse(
        Long id,
        Long adminId,
        String adminName,
        AdminAction action,
        String actionLabel,
        String targetType,
        Long targetId,
        String detail,
        LocalDateTime createdAt
) {

    public static AdminLogResponse of(AdminLog log) {
        return new AdminLogResponse(
                log.getId(),
                log.getAdmin().getId(),
                log.getAdmin().getName(),
                log.getAction(),
                log.getAction().getLabel(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
