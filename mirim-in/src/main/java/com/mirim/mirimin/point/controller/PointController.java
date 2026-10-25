package com.mirim.mirimin.point.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.point.dto.PointHistoryResponse;
import com.mirim.mirimin.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "포인트")
@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @Operation(summary = "내 포인트 내역", description = "최근 순. 누적 포인트는 /api/users/me 의 point")
    @GetMapping("/api/users/me/points")
    public PageResponse<PointHistoryResponse> myHistory(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return pointService.getMyHistory(authUser, page, size);
    }
}
