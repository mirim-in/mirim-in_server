package com.mirim.mirimin.admin.controller;

import com.mirim.mirimin.admin.dto.AdminUserResponse;
import com.mirim.mirimin.admin.dto.AdminVerificationRequestResponse;
import com.mirim.mirimin.admin.dto.DashboardResponse;
import com.mirim.mirimin.admin.dto.RejectRequest;
import com.mirim.mirimin.admin.dto.SuspendRequest;
import com.mirim.mirimin.admin.service.AdminDashboardService;
import com.mirim.mirimin.admin.service.AdminUserService;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** /api/admin/** 은 SecurityConfig에서 ADMIN만 허용한다. */
@Tag(name = "관리자 - 대시보드·회원")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminDashboardService dashboardService;
    private final AdminUserService adminUserService;

    @Operation(summary = "대시보드", description = "가입자·인증자·질문·답변 수, 채택률, 처리 대기 신고·승인 건수")
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }

    @Operation(summary = "회원 목록",
            description = "keyword는 이름·이메일. suspended=true면 정지 중인 회원만. 받은 신고 수(reportCount) 포함")
    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MemberType memberType,
            @RequestParam(required = false) Boolean suspended,
            @RequestParam(defaultValue = "LATEST") AdminUserService.UserSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminUserService.searchUsers(keyword, memberType, suspended, sort, page, size);
    }

    @Operation(summary = "회원 정지", description = "days가 없으면 영구 정지. 이미 정지 중이면 새 기간으로 바뀐다.")
    @PutMapping("/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable Long userId, @Valid @RequestBody SuspendRequest request,
                        @AuthenticationPrincipal AuthUser admin) {
        adminUserService.suspend(userId, request.days(), request.reason(), admin);
    }

    @Operation(summary = "정지 해제")
    @DeleteMapping("/users/{userId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsuspend(@PathVariable Long userId, @AuthenticationPrincipal AuthUser admin) {
        adminUserService.unsuspend(userId, admin);
    }

    @Operation(summary = "강제 탈퇴", description = "회원 탈퇴와 같이 개인정보를 지우고 글은 '탈퇴한 사용자'로 남긴다.")
    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceWithdraw(@PathVariable Long userId, @AuthenticationPrincipal AuthUser admin) {
        adminUserService.forceWithdraw(userId, admin);
    }

    @Operation(summary = "미림인 승인 신청 목록", description = "status 기본값 PENDING. keyword는 신청자 이름·이메일")
    @GetMapping("/verification-requests")
    public PageResponse<AdminVerificationRequestResponse> verificationRequests(
            @RequestParam(defaultValue = "PENDING") VerificationRequestStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminUserService.searchVerificationRequests(status, keyword, page, size);
    }

    @Operation(summary = "미림인 승인", description = "신청한 유형으로 회원 유형이 바뀐다.")
    @PostMapping("/verification-requests/{requestId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long requestId, @AuthenticationPrincipal AuthUser admin) {
        adminUserService.approve(requestId, admin);
    }

    @Operation(summary = "미림인 반려", description = "반려 사유는 신청자의 신청 내역에 보인다.")
    @PostMapping("/verification-requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long requestId, @Valid @RequestBody RejectRequest request,
                       @AuthenticationPrincipal AuthUser admin) {
        adminUserService.reject(requestId, request.reason(), admin);
    }
}
