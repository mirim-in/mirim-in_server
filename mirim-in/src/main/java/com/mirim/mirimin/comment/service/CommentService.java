package com.mirim.mirimin.comment.service;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.comment.dto.CommentRequest;
import com.mirim.mirimin.comment.dto.CommentResponse;
import com.mirim.mirimin.comment.entity.Comment;
import com.mirim.mirimin.comment.repository.CommentRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.notification.service.NotificationService;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** 댓글은 로그인한 사용자 누구나 쓸 수 있다. (정지 중이면 불가) */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserReader userReader;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CommentResponse> getQuestionComments(Long questionId, AuthUser authUser) {
        getPublishedQuestion(questionId);
        return toResponses(commentRepository.findByQuestionIdAndAnswerIsNullAndDeletedAtIsNullOrderByIdAsc(questionId),
                authUser);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getAnswerComments(Long answerId, AuthUser authUser) {
        getAnswer(answerId);
        return toResponses(commentRepository.findByAnswerIdAndDeletedAtIsNullOrderByIdAsc(answerId), authUser);
    }

    @Transactional
    public Long createOnQuestion(Long questionId, CommentRequest request, AuthUser authUser) {
        User author = userReader.getWritableUser(authUser);
        Question question = getPublishedQuestion(questionId);
        Comment comment = commentRepository.save(Comment.onQuestion(question, author, request.content()));
        notificationService.notifyCommentCreated(comment);
        return comment.getId();
    }

    @Transactional
    public Long createOnAnswer(Long answerId, CommentRequest request, AuthUser authUser) {
        User author = userReader.getWritableUser(authUser);
        Answer answer = getAnswer(answerId);
        Comment comment = commentRepository.save(Comment.onAnswer(answer, author, request.content()));
        notificationService.notifyCommentCreated(comment);
        return comment.getId();
    }

    @Transactional
    public void update(Long commentId, CommentRequest request, AuthUser authUser) {
        User user = userReader.getWritableUser(authUser);
        Comment comment = getComment(commentId);
        if (!comment.isAuthor(user)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        comment.update(request.content());
    }

    /** 작성자 또는 관리자 */
    @Transactional
    public void delete(Long commentId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Comment comment = getComment(commentId);
        if (!comment.isAuthor(user) && !user.isAdmin()) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        comment.delete(LocalDateTime.now(clock));
    }

    private List<CommentResponse> toResponses(List<Comment> comments, AuthUser authUser) {
        Long viewerId = authUser == null ? null : authUser.id();
        return comments.stream().map(c -> CommentResponse.of(c, viewerId)).toList();
    }

    private Question getPublishedQuestion(Long questionId) {
        return questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .filter(q -> !q.isDraft())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private Answer getAnswer(Long answerId) {
        return answerRepository.findByIdAndDeletedAtIsNull(answerId)
                .filter(a -> !a.getQuestion().isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
