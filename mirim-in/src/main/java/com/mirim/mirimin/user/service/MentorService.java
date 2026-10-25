package com.mirim.mirimin.user.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.user.dto.MentorResponse;
import com.mirim.mirimin.user.dto.MentorUpdateRequest;
import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserCategoryType;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MentorService {

    private final UserRepository userRepository;
    private final UserReader userReader;
    private final UserTagReader userTagReader;
    private final Clock clock;

    public enum MentorSort {
        LATEST(Sort.by(Sort.Order.desc("id"))),
        NAME(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));

        private final Sort sort;

        MentorSort(Sort sort) {
            this.sort = sort;
        }
    }

    /** 켜기: 2학년 이상 재학생, 졸업생, 교직원만. 끄기는 누구나. */
    @Transactional
    public void update(MentorUpdateRequest request, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        if (request.enabled() && !user.canBeMentor(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.NOT_MENTOR_ELIGIBLE);
        }
        user.changeMentorEnabled(request.enabled());
        if (request.intro() != null) {
            user.updateMentorIntro(request.intro().isBlank() ? null : request.intro().trim());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MentorResponse> search(Long categoryId, Department department, String keyword,
                                               MentorSort sort, int page, int size) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<User> mentors = userRepository.searchMentors(department, normalizedKeyword, categoryId,
                UserCategoryType.ANSWERABLE, PageRequest.of(page, size, sort.sort));

        Map<Long, UserTagReader.UserTags> tags = userTagReader.readAll(
                mentors.getContent().stream().map(User::getId).toList());
        return PageResponse.of(mentors, user -> MentorResponse.of(user,
                tags.getOrDefault(user.getId(), UserTagReader.UserTags.EMPTY).answerables()));
    }
}
