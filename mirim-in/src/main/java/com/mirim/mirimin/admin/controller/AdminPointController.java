package com.mirim.mirimin.admin.controller;

import com.mirim.mirimin.admin.dto.AdminLogResponse;
import com.mirim.mirimin.admin.dto.PointAdjustRequest;
import com.mirim.mirimin.admin.dto.PointPolicyRequest;
import com.mirim.mirimin.admin.dto.PointPolicyResponse;
import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.service.AdminPointService;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.point.dto.PointHistoryResponse;
import com.mirim.mirimin.point.entity.PointAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/** 은 SecurityConfig에서 ADMIN만 허용한다. */
@Tag(name = "관리자 - 포인트·로그")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPointController {

    private final AdminPointService adminPointService;

    @Operation(summary = "포인트 정책", description = "질문·답변·채택 시 지급하는 포인트")
    @GetMapping("/point-policies")
    public List<PointPolicyResponse> policies() {
        return adminPointService.getPolicies();
    }

    @Operation(summary = "포인트 정책 변경", description = "action: QUESTION, ANSWER, ACCEPTED. 바꾼 뒤의 활동부터 적용")
    @PutMapping("/point-policies/{action}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePolicy(@PathVariable PointAction action, @Valid @RequestBody PointPolicyRequest request,
                             @AuthenticationPrincipal AuthUser admin) {
        adminPointService.updatePolicy(action, request.amount(), admin);
    }

    @Operation(summary = "포인트 수동 지급·차감", description = "amount가 음수면 차감. 사유는 사용자 포인트 내역에도 보인다.")
    @PostMapping("/users/{userId}/points")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void adjust(@PathVariable Long userId, @Valid @RequestBody PointAdjustRequest request,
                       @AuthenticationPrincipal AuthUser admin) {
        adminPointService.adjust(userId, request.amount(), request.memo(), admin);
    }

    @Operation(summary = "회원 포인트 내역")
    @GetMapping("/users/{userId}/points")
    public PageResponse<PointHistoryResponse> userPoints(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminPointService.getUserHistory(userId, page, size);
    }

    @Operation(summary = "관리자 활동 로그",
            description = "action(조치 종류), targetType(USER, QUESTION, REPORT 등)으로 거른다. 최신 순")
    @GetMapping("/logs")
    public PageResponse<AdminLogResponse> logs(
            @RequestParam(required = false) AdminAction action,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminPointService.getLogs(action, targetType, page, size);
    }
}
