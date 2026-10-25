package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.dto.DashboardResponse;
import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.repository.ReportRepository;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;
import com.mirim.mirimin.user.repository.MemberVerificationRequestRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ReportRepository reportRepository;
    private final MemberVerificationRequestRepository verificationRequestRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long questions = questionRepository.countByDeletedAtIsNullAndPublishedAtIsNotNull();
        long resolved = questionRepository.countByDeletedAtIsNullAndStatus(QuestionStatus.RESOLVED);
        return new DashboardResponse(
                userRepository.countByDeletedAtIsNull(),
                userRepository.countByDeletedAtIsNullAndMemberTypeNot(MemberType.GENERAL),
                questions,
                answerRepository.countByDeletedAtIsNullAndQuestionDeletedAtIsNull(),
                questions == 0 ? 0 : (int) Math.round(resolved * 100.0 / questions),
                reportRepository.countByStatus(ReportStatus.PENDING),
                verificationRequestRepository.countByStatus(VerificationRequestStatus.PENDING)
        );
    }
}
