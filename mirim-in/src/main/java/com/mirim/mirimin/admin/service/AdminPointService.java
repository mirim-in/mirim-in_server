package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.dto.AdminLogResponse;
import com.mirim.mirimin.admin.dto.PointPolicyResponse;
import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.repository.AdminLogRepository;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.point.dto.PointHistoryResponse;
import com.mirim.mirimin.point.entity.PointAction;
import com.mirim.mirimin.point.entity.PointPolicy;
import com.mirim.mirimin.point.repository.PointPolicyRepository;
import com.mirim.mirimin.point.service.PointService;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** 포인트 정책 (AD-016), 수동 지급·차감 (AD-017), 관리자 활동 로그 조회 (AD-018) */
@Service
@RequiredArgsConstructor
public class AdminPointService {

    private final PointPolicyRepository policyRepository;
    private final PointService pointService;
    private final UserRepository userRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public List<PointPolicyResponse> getPolicies() {
        return Arrays.stream(PointAction.values())
                .map(action -> PointPolicyResponse.of(action, pointService.getAmount(action)))
                .toList();
    }

    /** 바꾼 뒤에 생기는 활동부터 적용된다. 이미 지급한 포인트는 그대로 둔다. */
    @Transactional
    public void updatePolicy(PointAction action, int amount, AuthUser admin) {
        PointPolicy policy = policyRepository.findByAction(action)
                .orElseGet(() -> policyRepository.save(new PointPolicy(action, amount)));
        int before = policy.getAmount();
        policy.changeAmount(amount);
        adminLogService.record(admin.id(), AdminAction.UPDATE_POINT_POLICY, "POINT_POLICY", policy.getId(),
                action.getLabel() + " " + before + " → " + amount);
    }

    @Transactional
    public void adjust(Long userId, int amount, String memo, AuthUser admin) {
        if (amount == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "0이 아닌 포인트를 입력해 주세요.");
        }
        userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        adminLogService.record(admin.id(), AdminAction.ADJUST_POINT, "USER", userId,
                (amount > 0 ? "+" : "") + amount + " - " + memo.trim());
        // 포인트 반영이 영속성 컨텍스트를 비우므로 마지막에 한다
        pointService.adjustByAdmin(userId, amount, memo.trim(), admin.id());
    }

    @Transactional(readOnly = true)
    public PageResponse<PointHistoryResponse> getUserHistory(Long userId, int page, int size) {
        userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return pointService.getHistory(userId, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminLogResponse> getLogs(AdminAction action, String targetType, int page, int size) {
        String normalizedType = targetType == null || targetType.isBlank() ? null : targetType.trim();
        return PageResponse.of(adminLogRepository.search(action, normalizedType, PageRequest.of(page, size)),
                AdminLogResponse::of);
    }
}
