package com.mirim.mirimin.point.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.point.dto.PointHistoryResponse;
import com.mirim.mirimin.point.entity.PointAction;
import com.mirim.mirimin.point.entity.PointHistory;
import com.mirim.mirimin.point.entity.PointPolicy;
import com.mirim.mirimin.point.entity.PointReason;
import com.mirim.mirimin.point.repository.PointHistoryRepository;
import com.mirim.mirimin.point.repository.PointPolicyRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 지급·회수. 금액은 point_policies 값을 따르고, 없으면 PointAction 기본값을 쓴다.
 * users.point를 DB에서 직접 증감하면서 영속성 컨텍스트를 비우므로, 서비스 메서드의 마지막에 호출해야 한다.
 */
@Service
@RequiredArgsConstructor
public class PointService {

    private static final List<PointReason> QUESTION_REWARDS = List.of(PointReason.QUESTION_CREATED);
    private static final List<PointReason> ANSWER_REWARDS =
            List.of(PointReason.ANSWER_CREATED, PointReason.ANSWER_ACCEPTED);

    private final PointPolicyRepository policyRepository;
    private final PointHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final UserReader userReader;
    private final Clock clock;

    @Transactional
    public void rewardQuestion(Long userId, Long questionId) {
        award(userId, PointAction.QUESTION, PointReason.QUESTION_CREATED, questionId);
    }

    @Transactional
    public void rewardAnswer(Long userId, Long answerId) {
        award(userId, PointAction.ANSWER, PointReason.ANSWER_CREATED, answerId);
    }

    @Transactional
    public void rewardAccepted(Long userId, Long answerId) {
        award(userId, PointAction.ACCEPTED, PointReason.ANSWER_ACCEPTED, answerId);
    }

    /** 질문 삭제 시 질문 작성 포인트를 회수한다. */
    @Transactional
    public void revokeQuestion(Long userId, Long questionId) {
        revoke(userId, questionId, QUESTION_REWARDS, PointReason.QUESTION_DELETED);
    }

    /** 답변 삭제 시 답변 작성 포인트와 채택 포인트를 함께 회수한다. */
    @Transactional
    public void revokeAnswer(Long userId, Long answerId) {
        revoke(userId, answerId, ANSWER_REWARDS, PointReason.ANSWER_DELETED);
    }

    @Transactional(readOnly = true)
    public PageResponse<PointHistoryResponse> getMyHistory(AuthUser authUser, int page, int size) {
        User user = userReader.getActiveUser(authUser);
        return PageResponse.of(historyRepository.findByUserIdOrderByIdDesc(user.getId(), PageRequest.of(page, size)),
                PointHistoryResponse::of);
    }

    private void award(Long userId, PointAction action, PointReason reason, Long referenceId) {
        int amount = policyRepository.findByAction(action).map(PointPolicy::getAmount).orElse(action.getDefaultAmount());
        if (amount != 0) {
            record(userId, amount, reason, referenceId);
        }
    }

    private void revoke(Long userId, Long referenceId, List<PointReason> rewardReasons, PointReason revokeReason) {
        int earned = historyRepository.sumAmount(userId, referenceId, rewardReasons);
        if (earned != 0) {
            record(userId, -earned, revokeReason, referenceId);
        }
    }

    private void record(Long userId, int amount, PointReason reason, Long referenceId) {
        historyRepository.save(PointHistory.builder()
                .user(userRepository.getReferenceById(userId))
                .amount(amount)
                .reason(reason)
                .referenceId(referenceId)
                .now(LocalDateTime.now(clock))
                .build());
        userRepository.addPoint(userId, amount);
    }
}
