package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.dto.AdminCategoryResponse;
import com.mirim.mirimin.admin.dto.CategoryRequest;
import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/** 카테고리 관리 (AD-010). 공식 카테고리 추가·수정·삭제, 사용자 카테고리 정리·공식 전환 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private static final String CATEGORY = "CATEGORY";

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AdminLogService adminLogService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<AdminCategoryResponse> search(Boolean official, String keyword, int page, int size) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Category> categories = categoryRepository.searchForAdmin(official, normalizedKeyword,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("official"), Sort.Order.asc("sortOrder"),
                        Sort.Order.asc("id"))));

        Map<Long, Long> questionCounts = categories.isEmpty() ? Map.of() : categoryRepository
                .countQuestions(categories.getContent().stream().map(Category::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return PageResponse.of(categories, c -> AdminCategoryResponse.of(c, questionCounts.getOrDefault(c.getId(), 0L)));
    }

    /**
     * 같은 이름이 사용자 카테고리로 있으면 공식으로 올리고, 삭제된 이름이면 되살린다.
     * 이미 공식 카테고리면 중복 오류
     */
    @Transactional
    public Long create(CategoryRequest request, AuthUser admin) {
        String name = request.name().trim();
        int sortOrder = request.sortOrder() != null ? request.sortOrder() : nextSortOrder();
        Category category = categoryRepository.findByName(name).map(existing -> {
            if (existing.isDeleted()) {
                existing.restoreAsOfficial(sortOrder);
            } else if (existing.isOfficial()) {
                throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY);
            } else {
                existing.promoteToOfficial(sortOrder);
            }
            return existing;
        }).orElseGet(() -> categoryRepository.save(
                Category.official(name, sortOrder, userRepository.getReferenceById(admin.id()))));

        adminLogService.record(admin.id(), AdminAction.CREATE_CATEGORY, CATEGORY, category.getId(), name);
        return category.getId();
    }

    @Transactional
    public void update(Long categoryId, CategoryRequest request, AuthUser admin) {
        Category category = getCategory(categoryId);
        String name = request.name().trim();
        if (!name.equals(category.getName())) {
            categoryRepository.findByName(name).ifPresent(other -> {
                throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY);
            });
            category.rename(name);
        }
        if (request.sortOrder() != null) {
            category.changeSortOrder(request.sortOrder());
        }
        adminLogService.record(admin.id(), AdminAction.UPDATE_CATEGORY, CATEGORY, categoryId, name);
    }

    /** 사용자 카테고리를 공식 카테고리로 전환 */
    @Transactional
    public void promote(Long categoryId, AuthUser admin) {
        Category category = getCategory(categoryId);
        if (!category.isOfficial()) {
            category.promoteToOfficial(nextSortOrder());
            adminLogService.record(admin.id(), AdminAction.UPDATE_CATEGORY, CATEGORY, categoryId, "공식 전환");
        }
    }

    /** 삭제된 카테고리는 질문 목록·상세, 필터, 분야 설정에서 사라진다. 질문 자체는 남는다. */
    @Transactional
    public void delete(Long categoryId, AuthUser admin) {
        Category category = getCategory(categoryId);
        category.delete(LocalDateTime.now(clock));
        adminLogService.record(admin.id(), AdminAction.DELETE_CATEGORY, CATEGORY, categoryId, category.getName());
    }

    private int nextSortOrder() {
        return categoryRepository.findByOfficialTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
                .mapToInt(Category::getSortOrder).max().orElse(0) + 1;
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
