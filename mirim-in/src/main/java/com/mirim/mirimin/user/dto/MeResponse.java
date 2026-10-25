package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.Role;
import com.mirim.mirimin.user.entity.User;

import java.time.LocalDate;

/**
 * 헤더·사이드바에 표시할 로그인 사용자 정보.
 * verified: '미림 구성원 인증' 배지 표시 여부
 * writeRestricted: 정지 중이라 글쓰기가 막혀 있는지
 */
public record MeResponse(
        Long id,
        String email,
        String name,
        Role role,
        MemberType memberType,
        String memberTypeLabel,
        boolean verified,
        Department department,
        String departmentLabel,
        Integer grade,
        Integer classNo,
        String profileImageUrl,
        int point,
        boolean mentorEnabled,
        boolean canBeMentor,
        boolean writeRestricted
) {

    public static MeResponse of(User user, LocalDate today, boolean writeRestricted) {
        Department department = user.getDepartment();
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getMemberType(),
                user.getMemberType().getLabel(),
                user.isVerifiedMember(),
                department,
                department != null ? department.getLabel() : null,
                user.getGrade(today),
                user.getClassNo(),
                user.getProfileImageUrl(),
                user.getPoint(),
                user.isMentorEnabled(),
                user.canBeMentor(today),
                writeRestricted
        );
    }
}
