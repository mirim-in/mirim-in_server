package com.mirim.mirimin.notice.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.notice.dto.NoticeDetailResponse;
import com.mirim.mirimin.notice.dto.NoticeSummaryResponse;
import com.mirim.mirimin.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "공지")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록", description = "사이드바 공지 탭. 고정 공지 먼저, 그다음 최신 순")
    @GetMapping
    public PageResponse<NoticeSummaryResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return noticeService.getPublished(page, size);
    }

    @Operation(summary = "고정 공지", description = "질문 목록 상단에 보여줄 공지")
    @GetMapping("/pinned")
    public List<NoticeSummaryResponse> pinned() {
        return noticeService.getPinned();
    }

    @Operation(summary = "공지 상세")
    @GetMapping("/{noticeId}")
    public NoticeDetailResponse detail(@PathVariable Long noticeId, @AuthenticationPrincipal AuthUser authUser) {
        return noticeService.getDetail(noticeId, authUser);
    }
}
