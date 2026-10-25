package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.User;

import java.util.List;

/** 멘토 카드. answerables: 답변 가능 분야 (카드 하단 태그) */
public record MentorResponse(
        Long id,
        String name,
        String profileImageUrl,
        MemberType memberType,
        String memberTypeLabel,
        boolean verified,
        String departmentLabel,
        String intro,
        List<CategoryResponse> answerables
) {

    public static MentorResponse of(User user, List<CategoryResponse> answerables) {
        return new MentorResponse(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getMemberType(),
                user.getMemberType().getLabel(),
                user.isVerifiedMember(),
                user.getDepartment() != null ? user.getDepartment().getLabel() : null,
                user.getMentorIntro(),
                answerables
        );
    }
}
