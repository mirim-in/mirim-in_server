package com.mirim.mirimin.user.service;

import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.category.repository.CategoryRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.image.service.ImagePurpose;
import com.mirim.mirimin.image.service.ImageService;
import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.user.dto.ProfileResponse;
import com.mirim.mirimin.user.dto.ProfileUpdateRequest;
import com.mirim.mirimin.user.dto.TechStacksRequest;
import com.mirim.mirimin.user.dto.UserAnswerResponse;
import com.mirim.mirimin.user.dto.UserCategoriesRequest;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserCategory;
import com.mirim.mirimin.user.entity.UserCategoryType;
import com.mirim.mirimin.user.entity.UserTechStack;
import com.mirim.mirimin.user.repository.UserCategoryRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int REPRESENTATIVE_ANSWER_COUNT = 3;
    private static final Sort LATEST = Sort.by(Sort.Order.desc("id"));

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserTagReader userTagReader;
    private final UserReader userReader;
    private final ImageService imageService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId, AuthUser viewer) {
        User user = getUser(userId);
        boolean mine = viewer != null && viewer.id().equals(userId);

        long questionCount = questionRepository.findByAuthor(userId, mine, PageRequest.of(0, 1)).getTotalElements();
        Object[] answerStats = answerRepository.findStats(userId).get(0);
        ProfileResponse.Stats stats = ProfileResponse.Stats.of(questionCount,
                ((Number) answerStats[0]).longValue(), ((Number) answerStats[1]).longValue(),
                ((Number) answerStats[2]).longValue(), user.getPoint());

        // 대표 답변: 채택된 답변 → 추천 많은 순
        List<UserAnswerResponse> representative = answerRepository.findByAuthor(userId,
                        PageRequest.of(0, REPRESENTATIVE_ANSWER_COUNT, Sort.by(
                                Sort.Order.desc("accepted"), Sort.Order.desc("likeCount"), Sort.Order.desc("id"))))
                .map(UserAnswerResponse::of).getContent();

        return ProfileResponse.of(user, mine, LocalDate.now(clock), userTagReader.read(userId), stats, representative);
    }

    /** 본인이 아니면 익명 질문은 빠진다. */
    @Transactional(readOnly = true)
    public PageResponse<QuestionSummaryResponse> getQuestions(Long userId, AuthUser viewer, int page, int size) {
        getUser(userId);
        boolean mine = viewer != null && viewer.id().equals(userId);
        return PageResponse.of(questionRepository.findByAuthor(userId, mine,
                        PageRequest.of(page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")))),
                QuestionSummaryResponse::of);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAnswerResponse> getAnswers(Long userId, int page, int size) {
        getUser(userId);
        return PageResponse.of(answerRepository.findByAuthor(userId, PageRequest.of(page, size, LATEST)),
                UserAnswerResponse::of);
    }

    @Transactional
    public void updateProfile(ProfileUpdateRequest request, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        user.updateProfile(request.name().trim(), request.classNo(), request.bio());
        user.changeDepartment(request.department());
    }

    @Transactional
    public void updateCategories(UserCategoriesRequest request, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        if (!request.answerableIdsOrEmpty().isEmpty() && !user.isVerifiedMember()) {
            throw new BusinessException(ErrorCode.ANSWERABLE_REQUIRES_VERIFIED);
        }
        List<Category> interests = getOfficialCategories(request.interestIdsOrEmpty());
        List<Category> answerables = getOfficialCategories(request.answerableIdsOrEmpty());

        userCategoryRepository.deleteByUserIdAndType(user.getId(), UserCategoryType.INTEREST);
        userCategoryRepository.deleteByUserIdAndType(user.getId(), UserCategoryType.ANSWERABLE);
        User ref = userRepository.getReferenceById(user.getId());
        interests.forEach(c -> userCategoryRepository.save(new UserCategory(ref, c, UserCategoryType.INTEREST)));
        answerables.forEach(c -> userCategoryRepository.save(new UserCategory(ref, c, UserCategoryType.ANSWERABLE)));
    }

    @Transactional
    public void updateTechStacks(TechStacksRequest request, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        // 대소문자만 다른 중복 제거 (처음 입력한 표기 유지)
        Map<String, String> unique = new LinkedHashMap<>();
        request.namesOrEmpty().forEach(raw -> unique.putIfAbsent(raw.trim().toLowerCase(Locale.ROOT), raw.trim()));

        userTechStackRepository.deleteByUserId(user.getId());
        User ref = userRepository.getReferenceById(user.getId());
        unique.values().forEach(name -> userTechStackRepository.save(new UserTechStack(ref, name)));
    }

    @Transactional
    public String updateProfileImage(MultipartFile file, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        String url = imageService.upload(file, ImagePurpose.PROFILE);
        user.updateProfileImage(url);
        return url;
    }

    @Transactional
    public void deleteProfileImage(AuthUser authUser) {
        userReader.getActiveUser(authUser).updateProfileImage(null);
    }

    private List<Category> getOfficialCategories(List<Long> ids) {
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        List<Category> categories = categoryRepository.findAllById(uniqueIds).stream()
                .filter(c -> c.isOfficial() && !c.isDeleted())
                .toList();
        if (categories.size() != uniqueIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_CATEGORY);
        }
        return categories;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
