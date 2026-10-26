package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.dto.AdminReportResponse;
import com.mirim.mirimin.admin.dto.ReportProcessRequest;
import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.comment.repository.CommentRepository;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.entity.ReportTargetType;
import com.mirim.mirimin.report.repository.ReportRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** 신고 처리 (AD-012, AD-013, RP-002) */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final List<ReportStatus> OPEN = List.of(ReportStatus.PENDING, ReportStatus.ON_HOLD);

    private final ReportRepository reportRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CommentRepository commentRepository;
    private final AdminUserService adminUserService;
    private final AdminContentService adminContentService;
    private final AdminLogService adminLogService;
    private final UserReader userReader;
    private final Clock clock;

    /** 신고된 글의 부모 질문 id와 삭제 여부 */
    private record TargetInfo(Long questionId, boolean deleted) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminReportResponse> search(ReportStatus status, ReportTargetType targetType,
                                                    ReportReason reason, int page, int size) {
        Page<Report> reports = reportRepository.search(status, targetType, reason,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("id"))));
        return PageResponse.of(reports, r -> {
            TargetInfo info = targetInfo(r.getTargetType(), r.getTargetId());
            return AdminReportResponse.of(r, info.questionId(), info.deleted());
        });
    }

    /**
     * 상태를 바꾸고, 고른 조치(작성자 정지, 글 삭제)를 함께 적용한다.
     * 글을 삭제하면 같은 글에 대한 다른 미처리 신고도 처리 완료로 바꾼다.
     */
    @Transactional
    public void process(Long reportId, ReportProcessRequest request, AuthUser authAdmin) {
        if (request.status() == ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "보류 또는 처리 완료를 선택해 주세요.");
        }
        User admin = userReader.getActiveUser(authAdmin);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        if (request.status() == ReportStatus.ON_HOLD) {
            report.hold(admin, now);
        } else {
            report.complete(admin, now);
        }

        boolean deleteContent = request.deleteContent()
                && !targetInfo(report.getTargetType(), report.getTargetId()).deleted();
        if (deleteContent) {
            reportRepository.findByTargetTypeAndTargetIdAndStatusIn(report.getTargetType(), report.getTargetId(), OPEN)
                    .forEach(other -> other.complete(admin, now));
        }
        adminLogService.record(admin.getId(), AdminAction.PROCESS_REPORT, "REPORT", reportId, request.status().name());

        if (request.suspends()) {
            adminUserService.suspend(report.getTargetUser().getId(),
                    request.permanentSuspend() ? null : request.suspendDays(), "신고 #" + reportId, authAdmin);
        }
        // 삭제는 영속성 컨텍스트를 비울 수 있으므로 마지막에 한다
        if (deleteContent) {
            deleteTarget(report.getTargetType(), report.getTargetId(), authAdmin);
        }
    }

    private void deleteTarget(ReportTargetType type, Long targetId, AuthUser admin) {
        switch (type) {
            case QUESTION -> adminContentService.deleteQuestion(targetId, admin);
            case ANSWER -> adminContentService.deleteAnswer(targetId, admin);
            case COMMENT -> adminContentService.deleteComment(targetId, admin);
        }
    }

    private TargetInfo targetInfo(ReportTargetType type, Long targetId) {
        return switch (type) {
            case QUESTION -> questionRepository.findById(targetId)
                    .map(q -> new TargetInfo(q.getId(), q.isDeleted()))
                    .orElse(new TargetInfo(null, true));
            case ANSWER -> answerRepository.findById(targetId)
                    .map(a -> new TargetInfo(a.getQuestion().getId(), a.isDeleted() || a.getQuestion().isDeleted()))
                    .orElse(new TargetInfo(null, true));
            case COMMENT -> commentRepository.findById(targetId)
                    .map(c -> new TargetInfo(c.getQuestion().getId(), c.isDeleted() || c.getQuestion().isDeleted()))
                    .orElse(new TargetInfo(null, true));
        };
    }
}
