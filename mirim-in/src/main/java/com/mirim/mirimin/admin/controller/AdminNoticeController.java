package com.mirim.mirimin.admin.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.faq.dto.FaqRequest;
import com.mirim.mirimin.faq.service.FaqService;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.notice.dto.NoticeRequest;
import com.mirim.mirimin.notice.dto.NoticeSummaryResponse;
import com.mirim.mirimin.notice.entity.NoticeStatus;
import com.mirim.mirimin.notice.service.NoticeService;
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
@Tag(name = "관리자 - 공지·FAQ")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final FaqService faqService;

    // ===== 공지 =====

    @Operation(summary = "공지 목록", description = "status=DRAFT면 임시 보관함. keyword는 제목·내용")
    @GetMapping("/notices")
    public PageResponse<NoticeSummaryResponse> notices(
            @RequestParam(required = false) NoticeStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return noticeService.searchForAdmin(status, keyword, page, size);
    }

    @Operation(summary = "공지 작성", description = "draft=false로 등록하면 전체 회원에게 알림이 간다. 응답은 공지 id")
    @PostMapping("/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createNotice(@Valid @RequestBody NoticeRequest request, @AuthenticationPrincipal AuthUser admin) {
        return noticeService.create(request, admin);
    }

    @Operation(summary = "공지 수정", description = "임시 보관 공지를 draft=false로 수정하면 등록되고 알림이 간다.")
    @PutMapping("/notices/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotice(@PathVariable Long noticeId, @Valid @RequestBody NoticeRequest request,
                             @AuthenticationPrincipal AuthUser admin) {
        noticeService.update(noticeId, request, admin);
    }

    @Operation(summary = "공지 삭제")
    @DeleteMapping("/notices/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(@PathVariable Long noticeId, @AuthenticationPrincipal AuthUser admin) {
        noticeService.delete(noticeId, admin);
    }

    // ===== FAQ =====

    @Operation(summary = "FAQ 등록", description = "categoryId는 공식 카테고리(없어도 됨). 응답은 FAQ id")
    @PostMapping("/faqs")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createFaq(@Valid @RequestBody FaqRequest request, @AuthenticationPrincipal AuthUser admin) {
        return faqService.create(request, admin);
    }

    @Operation(summary = "FAQ 수정", description = "sortOrder를 비우면 기존 순서를 유지한다.")
    @PutMapping("/faqs/{faqId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFaq(@PathVariable Long faqId, @Valid @RequestBody FaqRequest request,
                          @AuthenticationPrincipal AuthUser admin) {
        faqService.update(faqId, request, admin);
    }

    @Operation(summary = "FAQ 삭제")
    @DeleteMapping("/faqs/{faqId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFaq(@PathVariable Long faqId, @AuthenticationPrincipal AuthUser admin) {
        faqService.delete(faqId, admin);
    }
}
