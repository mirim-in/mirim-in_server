package com.mirim.mirimin.report.service;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.comment.entity.Comment;
import com.mirim.mirimin.comment.repository.CommentRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.report.dto.ReportRequest;
import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.repository.ReportRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 접수. 처리(보류·완료·정지 조치)는 관리자 기능에서 한다.
 * 원글이 수정·삭제돼도 확인할 수 있게 신고 시점의 내용을 저장한다.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final UserReader userReader;

    private record Target(User author, String content) {
    }

    @Transactional
    public Long report(ReportRequest request, AuthUser authUser) {
        User reporter = userReader.getActiveUser(authUser);
        String detail = request.detail() == null || request.detail().isBlank() ? null : request.detail().trim();
        if (request.reasons().contains(ReportReason.ETC) && detail == null) {
            throw new BusinessException(ErrorCode.REPORT_DETAIL_REQUIRED);
        }

        Target target = findTarget(request);
        if (target.author().getId().equals(reporter.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_OWN);
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(), request.targetType(), request.targetId())) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }

        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .targetType(request.targetType())
                .targetId(request.targetId())
                .targetUser(target.author())
                .reasons(request.reasons())
                .detail(detail)
                .contentSnapshot(target.content())
                .build()).getId();
    }

    private Target findTarget(ReportRequest request) {
        Long id = request.targetId();
        return switch (request.targetType()) {
            case QUESTION -> {
                Question q = questionRepository.findByIdAndDeletedAtIsNull(id)
                        .filter(question -> !question.isDraft())
                        .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
                yield new Target(q.getAuthor(), q.getTitle() + "\n\n" + q.getContent());
            }
            case ANSWER -> {
                Answer a = answerRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
                yield new Target(a.getAuthor(), a.getContent());
            }
            case COMMENT -> {
                Comment c = commentRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
                yield new Target(c.getAuthor(), c.getContent());
            }
        };
    }
}
