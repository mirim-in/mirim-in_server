package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserTagReader;

import java.time.LocalDate;
import java.util.List;

/**
 * 프로필 화면.
 * email: 본인 프로필일 때만 채워진다.
 * stats.acceptRate: 채택 수 / 답변 수 (%, 반올림). 답변이 없으면 0
 */
public record ProfileResponse(
        Long id,
        String name,
        String email,
        String profileImageUrl,
        MemberType memberType,
        String memberTypeLabel,
        boolean verified,
        Department department,
        String departmentLabel,
        Integer grade,
        Integer classNo,
        String bio,
        boolean mentorEnabled,
        String mentorIntro,
        List<CategoryResponse> interests,
        List<CategoryResponse> answerables,
        List<String> techStacks,
        Stats stats,
        List<UserAnswerResponse> representativeAnswers,
        boolean mine
) {

    public record Stats(long questionCount, long answerCount, long acceptedCount, int acceptRate,
                        long likeCount, int point) {

        public static Stats of(long questionCount, long answerCount, long acceptedCount, long likeCount, int point) {
            int rate = answerCount == 0 ? 0 : (int) Math.round(acceptedCount * 100.0 / answerCount);
            return new Stats(questionCount, answerCount, acceptedCount, rate, likeCount, point);
        }
    }

    public static ProfileResponse of(User user, boolean mine, LocalDate today, UserTagReader.UserTags tags,
                                     Stats stats, List<UserAnswerResponse> representativeAnswers) {
        Department department = user.getDepartment();
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                mine ? user.getEmail() : null,
                user.getProfileImageUrl(),
                user.getMemberType(),
                user.getMemberType().getLabel(),
                user.isVerifiedMember(),
                department,
                department != null ? department.getLabel() : null,
                user.getGrade(today),
                user.getClassNo(),
                user.getBio(),
                user.isMentorEnabled(),
                user.getMentorIntro(),
                tags.interests(),
                tags.answerables(),
                tags.techStacks(),
                stats,
                representativeAnswers,
                mine
        );
    }
}
