package com.mirim.mirimin.question.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.entity.Bookmark;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionCuriosity;
import com.mirim.mirimin.question.repository.BookmarkRepository;
import com.mirim.mirimin.question.repository.QuestionCuriosityRepository;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "저도 궁금해요", 즐겨찾기. 추가/취소는 여러 번 호출해도 결과가 같다. */
@Service
@RequiredArgsConstructor
public class QuestionReactionService {

    private final QuestionRepository questionRepository;
    private final QuestionCuriosityRepository curiosityRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserReader userReader;

    @Transactional
    public void addCuriosity(Long questionId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Question question = getPublishedQuestion(questionId);
        if (question.isAuthor(user)) {
            throw new BusinessException(ErrorCode.CANNOT_CURIOUS_OWN_QUESTION);
        }
        if (curiosityRepository.existsByUserIdAndQuestionId(user.getId(), questionId)) {
            return;
        }
        curiosityRepository.save(new QuestionCuriosity(user, question));
        questionRepository.addCuriosityCount(questionId, 1);
    }

    @Transactional
    public void removeCuriosity(Long questionId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        curiosityRepository.findByUserIdAndQuestionId(user.getId(), questionId).ifPresent(curiosity -> {
            curiosityRepository.delete(curiosity);
            questionRepository.addCuriosityCount(questionId, -1);
        });
    }

    @Transactional
    public void addBookmark(Long questionId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Question question = getPublishedQuestion(questionId);
        if (!bookmarkRepository.existsByUserIdAndQuestionId(user.getId(), questionId)) {
            bookmarkRepository.save(new Bookmark(user, question));
        }
    }

    @Transactional
    public void removeBookmark(Long questionId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        bookmarkRepository.findByUserIdAndQuestionId(user.getId(), questionId).ifPresent(bookmarkRepository::delete);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionSummaryResponse> getMyBookmarks(AuthUser authUser, int page, int size) {
        User user = userReader.getActiveUser(authUser);
        return PageResponse.of(bookmarkRepository.findMyBookmarks(user.getId(), PageRequest.of(page, size)),
                bookmark -> QuestionSummaryResponse.of(bookmark.getQuestion()));
    }

    private Question getPublishedQuestion(Long questionId) {
        return questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .filter(q -> !q.isDraft())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }
}
