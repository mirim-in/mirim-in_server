package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.User;

/** 질문·답변·댓글 작성자 표시용. verified면 '미림 구성원 인증' 배지를 붙인다. */
public record AuthorResponse(
        Long id,
        String name,
        String profileImageUrl,
        MemberType memberType,
        String memberTypeLabel,
        boolean verified,
        String departmentLabel,
        boolean withdrawn
) {

    private static final String WITHDRAWN_NAME = "탈퇴한 사용자";

    public static AuthorResponse of(User user) {
        if (user.isDeleted()) {
            return new AuthorResponse(null, WITHDRAWN_NAME, null, null, null, false, null, true);
        }
        return new AuthorResponse(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getMemberType(),
                user.getMemberType().getLabel(),
                user.isVerifiedMember(),
                user.getDepartment() != null ? user.getDepartment().getLabel() : null,
                false
        );
    }
}
