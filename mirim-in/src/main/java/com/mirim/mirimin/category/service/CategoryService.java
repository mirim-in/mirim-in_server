package com.mirim.mirimin.category.service;

import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    public static final int MAX_CATEGORIES_PER_QUESTION = 5;

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> getOfficialCategories() {
        return categoryRepository.findByOfficialTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc();
    }

    /**
     * 질문에 붙일 카테고리를 확정한다.
     * - officialIds: 칩에서 고른 공식 카테고리
     * - newNames: "+추가"로 입력한 이름. 같은 이름이 이미 있으면 그것을 쓰고, 없으면 사용자 카테고리로 만든다.
     */
    @Transactional
    public List<Category> resolveForQuestion(List<Long> officialIds, List<String> newNames, User user) {
        Map<Long, Category> result = new LinkedHashMap<>();

        for (Long id : officialIds) {
            Category category = categoryRepository.findById(id)
                    .filter(c -> c.isOfficial() && !c.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CATEGORY));
            result.put(category.getId(), category);
        }
        for (String rawName : newNames) {
            String name = rawName.trim();
            if (name.isEmpty()) {
                continue;
            }
            Category category = categoryRepository.findByName(name)
                    .orElseGet(() -> categoryRepository.save(Category.userCreated(name, user)));
            if (category.isDeleted()) {
                throw new BusinessException(ErrorCode.INVALID_CATEGORY, "'" + name + "'은(는) 사용할 수 없는 카테고리예요.");
            }
            result.put(category.getId(), category);
        }

        if (result.size() > MAX_CATEGORIES_PER_QUESTION) {
            throw new BusinessException(ErrorCode.TOO_MANY_CATEGORIES);
        }
        return new ArrayList<>(result.values());
    }
}
