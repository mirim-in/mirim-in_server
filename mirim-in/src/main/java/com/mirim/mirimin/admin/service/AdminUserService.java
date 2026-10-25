package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.dto.AdminUserResponse;
import com.mirim.mirimin.admin.dto.AdminVerificationRequestResponse;
import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.report.repository.ReportRepository;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.MemberVerificationRequest;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserStatus;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;
import com.mirim.mirimin.user.repository.MemberVerificationRequestRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.service.UserReader;
import com.mirim.mirimin.user.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/** 회원 관리(정지·해제·강제 탈퇴)와 미림인 승인. 관리자 계정에는 정지·강제 탈퇴를 적용할 수 없다. */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String USER = "USER";
    private static final String VERIFICATION_REQUEST = "VERIFICATION_REQUEST";

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final MemberVerificationRequestRepository verificationRequestRepository;
    private final WithdrawalService withdrawalService;
    private final AdminLogService adminLogService;
    private final UserReader userReader;
    private final Clock clock;

    public enum UserSort {
        LATEST(Sort.by(Sort.Order.desc("id"))),
        NAME(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));

        private final Sort sort;

        UserSort(Sort sort) {
            this.sort = sort;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> searchUsers(String keyword, MemberType memberType, Boolean suspended,
                                                       UserSort sort, int page, int size) {
        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<User> users = userRepository.searchForAdmin(normalizedKeyword, memberType, suspended,
                UserStatus.SUSPENDED, now, PageRequest.of(page, size, sort.sort));

        Map<Long, Long> reportCounts = users.isEmpty() ? Map.of() : reportRepository
                .countByTargetUsers(users.getContent().stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return PageResponse.of(users, u -> AdminUserResponse.of(u, u.isWriteRestricted(now),
                reportCounts.getOrDefault(u.getId(), 0L)));
    }

    /** days가 null이면 영구 정지 */
    @Transactional
    public void suspend(Long userId, Integer days, String reason, AuthUser admin) {
        User user = getManageableUser(userId);
        LocalDateTime until = days == null ? null : LocalDateTime.now(clock).plusDays(days);
        user.suspend(until);
        adminLogService.record(admin.id(), AdminAction.SUSPEND_USER, USER, userId,
                (days == null ? "영구 정지" : days + "일 정지") + (reason == null ? "" : " - " + reason));
    }

    @Transactional
    public void unsuspend(Long userId, AuthUser admin) {
        getManageableUser(userId).unsuspend();
        adminLogService.record(admin.id(), AdminAction.UNSUSPEND_USER, USER, userId, null);
    }

    @Transactional
    public void forceWithdraw(Long userId, AuthUser admin) {
        User user = getManageableUser(userId);
        String email = user.getEmail();
        withdrawalService.withdraw(user);
        // 탈퇴하면 이메일이 지워지므로 기록에 원래 이메일을 남긴다
        adminLogService.record(admin.id(), AdminAction.FORCE_WITHDRAW, USER, userId, email);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminVerificationRequestResponse> searchVerificationRequests(
            VerificationRequestStatus status, String keyword, int page, int size) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.of(verificationRequestRepository.search(status, normalizedKeyword,
                        PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")))),
                AdminVerificationRequestResponse::of);
    }

    /** 승인하면 신청한 유형으로 회원 유형이 바뀌고 '미림 구성원 인증' 배지가 붙는다. */
    @Transactional
    public void approve(Long requestId, AuthUser authAdmin) {
        User admin = userReader.getActiveUser(authAdmin);
        MemberVerificationRequest request = getPendingRequest(requestId);
        request.approve(admin, LocalDateTime.now(clock));
        adminLogService.record(admin.getId(), AdminAction.APPROVE_MEMBER, VERIFICATION_REQUEST, requestId,
                request.getRequestedType().getLabel());
    }

    @Transactional
    public void reject(Long requestId, String reason, AuthUser authAdmin) {
        User admin = userReader.getActiveUser(authAdmin);
        MemberVerificationRequest request = getPendingRequest(requestId);
        request.reject(admin, reason.trim(), LocalDateTime.now(clock));
        adminLogService.record(admin.getId(), AdminAction.REJECT_MEMBER, VERIFICATION_REQUEST, requestId, reason);
    }

    private MemberVerificationRequest getPendingRequest(Long requestId) {
        MemberVerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_REQUEST_NOT_FOUND));
        if (request.getStatus() != VerificationRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_REVIEWED);
        }
        if (request.getUser().isDeleted()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return request;
    }

    private User getManageableUser(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isAdmin()) {
            throw new BusinessException(ErrorCode.CANNOT_MANAGE_ADMIN);
        }
        return user;
    }
}
