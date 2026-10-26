package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.dto.AdminAnswerResponse;
import com.mirim.mirimin.admin.dto.AdminQuestionResponse;
import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.answer.service.AnswerService;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.comment.service.CommentService;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 콘텐츠 관리. 삭제는 각 도메인 서비스의 삭제(포인트 회수 등 포함)를 그대로 쓰고 기록만 남긴다.
 * 도메인 서비스의 삭제가 영속성 컨텍스트를 비울 수 있으므로 기록을 먼저 남긴다.
 */
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private static final Sort LATEST = Sort.by(Sort.Order.desc("id"));

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final CommentService commentService;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public PageResponse<AdminQuestionResponse> searchQuestions(String keyword, QuestionStatus status,
                                                               int page, int size) {
        return PageResponse.of(questionRepository.search(status, null, normalize(keyword),
                PageRequest.of(page, size, LATEST)), AdminQuestionResponse::of);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAnswerResponse> searchAnswers(String keyword, int page, int size) {
        return PageResponse.of(answerRepository.searchForAdmin(normalize(keyword), PageRequest.of(page, size, LATEST)),
                AdminAnswerResponse::of);
    }

    @Transactional
    public void deleteQuestion(Long questionId, AuthUser admin) {
        adminLogService.record(admin.id(), AdminAction.DELETE_QUESTION, "QUESTION", questionId, null);
        questionService.delete(questionId, admin);
    }

    @Transactional
    public void deleteAnswer(Long answerId, AuthUser admin) {
        adminLogService.record(admin.id(), AdminAction.DELETE_ANSWER, "ANSWER", answerId, null);
        answerService.delete(answerId, admin);
    }

    @Transactional
    public void deleteComment(Long commentId, AuthUser admin) {
        adminLogService.record(admin.id(), AdminAction.DELETE_COMMENT, "COMMENT", commentId, null);
        commentService.delete(commentId, admin);
    }

    private String normalize(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
