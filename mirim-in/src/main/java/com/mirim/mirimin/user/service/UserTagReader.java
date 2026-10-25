package com.mirim.mirimin.user.service;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.user.entity.UserCategory;
import com.mirim.mirimin.user.entity.UserCategoryType;
import com.mirim.mirimin.user.entity.UserTechStack;
import com.mirim.mirimin.user.repository.UserCategoryRepository;
import com.mirim.mirimin.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관심 분야, 답변 가능 분야, 기술 스택을 여러 사용자분 한 번에 읽는다.
 * (답변 목록·멘토 목록에서 작성자마다 쿼리가 나가지 않도록)
 */
@Component
@RequiredArgsConstructor
public class UserTagReader {

    private final UserCategoryRepository userCategoryRepository;
    private final UserTechStackRepository userTechStackRepository;

    public record UserTags(List<CategoryResponse> interests, List<CategoryResponse> answerables,
                           List<String> techStacks) {

        public static final UserTags EMPTY = new UserTags(List.of(), List.of(), List.of());
    }

    public UserTags read(Long userId) {
        return readAll(List.of(userId)).getOrDefault(userId, UserTags.EMPTY);
    }

    public Map<Long, UserTags> readAll(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<UserCategory>> categories = userCategoryRepository.findByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(uc -> uc.getUser().getId()));
        Map<Long, List<String>> stacks = userTechStackRepository.findByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId(),
                        Collectors.mapping(UserTechStack::getName, Collectors.toList())));

        return userIds.stream().distinct().collect(Collectors.toMap(id -> id, id -> {
            List<UserCategory> mine = categories.getOrDefault(id, List.of());
            return new UserTags(
                    filter(mine, UserCategoryType.INTEREST),
                    filter(mine, UserCategoryType.ANSWERABLE),
                    stacks.getOrDefault(id, List.of()));
        }));
    }

    private List<CategoryResponse> filter(List<UserCategory> categories, UserCategoryType type) {
        return categories.stream()
                .filter(uc -> uc.getType() == type)
                .map(uc -> CategoryResponse.of(uc.getCategory()))
                .toList();
    }
}
