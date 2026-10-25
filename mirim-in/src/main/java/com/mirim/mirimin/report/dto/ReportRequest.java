package com.mirim.mirimin.report.dto;

import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * reasons: SLANDER(비방), PROFANITY(욕설), RUDE(불친절), OBSCENE(불건전), ADVERTISEMENT(홍보), ETC(기타)
 * ETC를 고르면 detail이 필요하다.
 */
public record ReportRequest(
        @NotNull(message = "신고 대상을 선택해 주세요.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상을 선택해 주세요.")
        Long targetId,

        @NotEmpty(message = "신고 사유를 하나 이상 선택해 주세요.")
        Set<@NotNull ReportReason> reasons,

        @Size(max = 500, message = "상세 내용은 500자 이하로 입력해 주세요.")
        String detail
) {
}
