package com.mirim.mirimin.admin.controller;

import com.mirim.mirimin.admin.dto.AdminAnswerResponse;
import com.mirim.mirimin.admin.dto.AdminCategoryResponse;
import com.mirim.mirimin.admin.dto.AdminQuestionResponse;
import com.mirim.mirimin.admin.dto.AdminReportResponse;
import com.mirim.mirimin.admin.dto.CategoryRequest;
import com.mirim.mirimin.admin.dto.ReportProcessRequest;
import com.mirim.mirimin.admin.service.AdminCategoryService;
import com.mirim.mirimin.admin.service.AdminContentService;
import com.mirim.mirimin.admin.service.AdminReportService;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.entity.ReportTargetType;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** /api/admin/** 은 SecurityConfig에서 ADMIN만 허용한다. */
@Tag(name = "관리자 - 신고·콘텐츠·카테고리")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminReportService reportService;
    private final AdminContentService contentService;
    private final AdminCategoryService categoryService;

    // ===== 신고 =====

    @Operation(summary = "신고 목록", description = "status(PENDING/ON_HOLD/DONE), targetType, reason 필터. 최신 순")
    @GetMapping("/reports")
    public PageResponse<AdminReportResponse> reports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) ReportReason reason,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return reportService.search(status, targetType, reason, page, size);
    }

    @Operation(summary = "신고 처리", description = "보류/처리 완료와 함께 작성자 정지, 글 삭제를 고를 수 있다.")
    @PatchMapping("/reports/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processReport(@PathVariable Long reportId, @Valid @RequestBody ReportProcessRequest request,
                              @AuthenticationPrincipal AuthUser admin) {
        reportService.process(reportId, request, admin);
    }

    // ===== 질문·답변·댓글 =====

    @Operation(summary = "질문 목록", description = "익명 질문도 실제 작성자(realAuthor)를 보여준다.")
    @GetMapping("/questions")
    public PageResponse<AdminQuestionResponse> questions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return contentService.searchQuestions(keyword, status, page, size);
    }

    @Operation(summary = "질문 삭제", description = "작성자의 질문 포인트도 회수된다.")
    @DeleteMapping("/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long questionId, @AuthenticationPrincipal AuthUser admin) {
        contentService.deleteQuestion(questionId, admin);
    }

    @Operation(summary = "답변 목록", description = "keyword는 답변 내용에서 찾는다.")
    @GetMapping("/answers")
    public PageResponse<AdminAnswerResponse> answers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return contentService.searchAnswers(keyword, page, size);
    }

    @Operation(summary = "답변 삭제", description = "채택된 답변이면 질문은 답변 대기로 돌아가고, 답변·채택 포인트가 회수된다.")
    @DeleteMapping("/answers/{answerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswer(@PathVariable Long answerId, @AuthenticationPrincipal AuthUser admin) {
        contentService.deleteAnswer(answerId, admin);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal AuthUser admin) {
        contentService.deleteComment(commentId, admin);
    }

    // ===== 카테고리 =====

    @Operation(summary = "카테고리 목록", description = "official=false면 사용자 카테고리만. 카테고리별 질문 수 포함")
    @GetMapping("/categories")
    public PageResponse<AdminCategoryResponse> categories(
            @RequestParam(required = false) Boolean official,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return categoryService.search(official, keyword, page, size);
    }

    @Operation(summary = "공식 카테고리 추가",
            description = "같은 이름의 사용자 카테고리가 있으면 공식으로 전환한다. 응답은 카테고리 id")
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createCategory(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal AuthUser admin) {
        return categoryService.create(request, admin);
    }

    @Operation(summary = "카테고리 이름·순서 수정")
    @PutMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request,
                               @AuthenticationPrincipal AuthUser admin) {
        categoryService.update(categoryId, request, admin);
    }

    @Operation(summary = "사용자 카테고리를 공식으로 전환", description = "공식 칩 맨 뒤에 추가된다.")
    @PostMapping("/categories/{categoryId}/promote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteCategory(@PathVariable Long categoryId, @AuthenticationPrincipal AuthUser admin) {
        categoryService.promote(categoryId, admin);
    }

    @Operation(summary = "카테고리 삭제", description = "질문은 남고, 질문 목록·상세와 필터에서 이 카테고리만 사라진다.")
    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId, @AuthenticationPrincipal AuthUser admin) {
        categoryService.delete(categoryId, admin);
    }
}
