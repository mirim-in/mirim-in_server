package com.mirim.mirimin.question.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.category.service.CategoryService;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.dto.PopularPeriod;
import com.mirim.mirimin.question.dto.QuestionDetailResponse;
import com.mirim.mirimin.question.dto.QuestionRequest;
import com.mirim.mirimin.question.dto.QuestionSort;
import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.question.entity.QuestionView;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.question.repository.QuestionViewRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    // 같은 사람이 이 시간 안에 다시 보면 조회수를 올리지 않는다
    private static final long VIEW_DEDUP_HOURS = 24;

    private final QuestionRepository questionRepository;
    private final QuestionViewRepository questionViewRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final UserReader userReader;
    private final Clock clock;

    @Transactional
    public Long create(QuestionRequest request, AuthUser authUser) {
        User author = userReader.getWritableUser(authUser);
        List<Category> categories = categoryService.resolveForQuestion(
                request.categoryIdsOrEmpty(), request.newCategoriesOrEmpty(), author);

        Question question = questionRepository.save(Question.builder()
                .author(author)
                .title(request.title().trim())
                .content(request.content())
                .anonymous(request.anonymous())
                .draft(request.draft())
                .now(LocalDateTime.now(clock))
                .build());
        question.changeCategories(categories);
        question.changeImages(request.imageUrlsOrEmpty());
        return question.getId();
    }

    @Transactional
    public void update(Long questionId, QuestionRequest request, AuthUser authUser) {
        User author = userReader.getWritableUser(authUser);
        Question question = getQuestion(questionId);
        if (!question.isAuthor(author)) {
            throw new BusinessException(ErrorCode.NOT_QUESTION_AUTHOR);
        }
        List<Category> categories = categoryService.resolveForQuestion(
                request.categoryIdsOrEmpty(), request.newCategoriesOrEmpty(), author);

        question.update(request.title().trim(), request.content(), request.anonymous());
        question.changeCategories(categories);
        question.changeImages(request.imageUrlsOrEmpty());
        if (!request.draft()) {
            question.publish(LocalDateTime.now(clock));
        }
    }

    /** 작성자 또는 관리자 */
    @Transactional
    public void delete(Long questionId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Question question = getQuestion(questionId);
        if (!question.isAuthor(user) && !user.isAdmin()) {
            throw new BusinessException(ErrorCode.NOT_QUESTION_AUTHOR);
        }
        question.delete(LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionSummaryResponse> search(Long categoryId, String keyword, QuestionStatus status,
                                                        QuestionSort sort, int page, int size) {
        if (status == QuestionStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Question> result = questionRepository.search(status, categoryId, normalizedKeyword,
                PageRequest.of(page, size, sort.toSort()));
        return PageResponse.of(result, QuestionSummaryResponse::of);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionSummaryResponse> popular(PopularPeriod period, int page, int size) {
        if (period == PopularPeriod.ALL) {
            return search(null, null, null, QuestionSort.VIEWS, page, size);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime from = period == PopularPeriod.WEEK ? now.minusDays(7) : now.minusDays(30);

        Page<Object[]> ranked = questionRepository.findPopularIds(from, PageRequest.of(page, size));
        List<Long> ids = ranked.getContent().stream().map(row -> (Long) row[0]).toList();
        Map<Long, Question> byId = questionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<QuestionSummaryResponse> content = ids.stream()
                .map(byId::get)
                .map(QuestionSummaryResponse::of)
                .toList();
        return PageResponse.of(ranked, content);
    }

    /**
     * 임시 보관 질문은 작성자만 볼 수 있다.
     * viewerKey: 로그인 "u:{id}", 비로그인 클라이언트 식별값. 24시간 안에 다시 보면 조회수를 올리지 않는다.
     */
    @Transactional
    public QuestionDetailResponse getDetail(Long questionId, AuthUser authUser, String viewerKey) {
        Question question = getQuestion(questionId);
        boolean mine = authUser != null && question.getAuthor().getId().equals(authUser.id());
        if (question.isDraft() && !mine) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }

        if (!question.isDraft()) {
            recordView(question, authUser, viewerKey);
        }
        return QuestionDetailResponse.of(question, questionRepository.findViewCount(questionId), mine);
    }

    @Transactional(readOnly = true)
    public List<QuestionSummaryResponse> getMyDrafts(AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        return questionRepository
                .findByAuthorIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(user.getId(), QuestionStatus.DRAFT)
                .stream().map(QuestionSummaryResponse::of).toList();
    }

    private void recordView(Question question, AuthUser authUser, String viewerKey) {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean viewedRecently = questionViewRepository.existsByQuestionIdAndViewerKeyAndViewedAtAfter(
                question.getId(), viewerKey, now.minusHours(VIEW_DEDUP_HOURS));
        if (viewedRecently) {
            return;
        }
        User viewer = authUser == null ? null : userRepository.getReferenceById(authUser.id());
        questionViewRepository.save(new QuestionView(question, viewer, viewerKey, now));
        questionRepository.increaseViewCount(question.getId());
    }

    private Question getQuestion(Long questionId) {
        return questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }
}
