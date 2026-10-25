package com.mirim.mirimin.notification.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.notification.dto.NotificationResponse;
import com.mirim.mirimin.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    public record UnreadCountResponse(long count) {
    }

    @Operation(summary = "내 알림 목록", description = "최근 순")
    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return notificationService.getMyNotifications(authUser, page, size);
    }

    @Operation(summary = "안 읽은 알림 수", description = "헤더·사이드바 알림 배지. 프론트에서 주기적으로 조회한다.")
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AuthUser authUser) {
        return new UnreadCountResponse(notificationService.countUnread(authUser));
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long notificationId, @AuthenticationPrincipal AuthUser authUser) {
        notificationService.markRead(notificationId, authUser);
    }

    @Operation(summary = "모두 읽음 처리")
    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthUser authUser) {
        notificationService.markAllRead(authUser);
    }
}
