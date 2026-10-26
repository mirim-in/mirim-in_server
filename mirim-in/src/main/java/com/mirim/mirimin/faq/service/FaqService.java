package com.mirim.mirimin.faq.service;

import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.service.AdminLogService;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.faq.dto.FaqRequest;
import com.mirim.mirimin.faq.dto.FaqResponse;
import com.mirim.mirimin.faq.entity.Faq;
import com.mirim.mirimin.faq.repository.FaqRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** FAQ (FQ-001, FQ-002, AD-011). 등록·수정·삭제는 관리자만, 조회는 누구나 */
@Service
@RequiredArgsConstructor
public class FaqService {

    private static final String FAQ = "FAQ";

    private final FaqRepository faqRepository;
    private final CategoryRepository categoryRepository;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqs(Long categoryId) {
        return faqRepository.findAllByCategory(categoryId).stream().map(FaqResponse::of).toList();
    }

    @Transactional
    public Long create(FaqRequest request, AuthUser admin) {
        Faq faq = faqRepository.save(new Faq(getCategory(request.categoryId()), request.question().trim(),
                request.answer(), sortOrderOf(request)));
        adminLogService.record(admin.id(), AdminAction.CREATE_FAQ, FAQ, faq.getId(), faq.getQuestion());
        return faq.getId();
    }

    @Transactional
    public void update(Long faqId, FaqRequest request, AuthUser admin) {
        Faq faq = getFaq(faqId);
        faq.update(getCategory(request.categoryId()), request.question().trim(), request.answer(),
                request.sortOrder() != null ? request.sortOrder() : faq.getSortOrder());
        adminLogService.record(admin.id(), AdminAction.UPDATE_FAQ, FAQ, faqId, faq.getQuestion());
    }

    @Transactional
    public void delete(Long faqId, AuthUser admin) {
        Faq faq = getFaq(faqId);
        adminLogService.record(admin.id(), AdminAction.DELETE_FAQ, FAQ, faqId, faq.getQuestion());
        faqRepository.delete(faq);
    }

    private int sortOrderOf(FaqRequest request) {
        if (request.sortOrder() != null) {
            return request.sortOrder();
        }
        return faqRepository.findAllByCategory(null).stream().mapToInt(Faq::getSortOrder).max().orElse(0) + 1;
    }

    /** 공식 카테고리만 */
    private Category getCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .filter(c -> c.isOfficial() && !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CATEGORY));
    }

    private Faq getFaq(Long faqId) {
        return faqRepository.findById(faqId).orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
    }
}
