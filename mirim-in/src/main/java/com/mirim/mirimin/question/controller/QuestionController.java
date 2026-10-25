package com.mirim.mirimin.question.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.auth.service.TokenHasher;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.dto.PopularPeriod;
import com.mirim.mirimin.question.dto.QuestionDetailResponse;
import com.mirim.mirimin.question.dto.QuestionRequest;
import com.mirim.mirimin.question.dto.QuestionSort;
import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.entity.QuestionStatus;
import com.mirim.mirimin.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

import java.util.List;

@Tag(name = "질문")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "질문 목록·검색",
            description = "keyword는 제목·내용·카테고리 이름에서 찾는다. status: OPEN(답변 대기), RESOLVED(채택 완료)")
    @GetMapping
    public PageResponse<QuestionSummaryResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(defaultValue = "LATEST") QuestionSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return questionService.search(categoryId, keyword, status, sort, page, size);
    }

    @Operation(summary = "인기 질문", description = "WEEK: 최근 7일 조회 수, MONTH: 최근 30일 조회 수, ALL: 누적 조회수")
    @GetMapping("/popular")
    public PageResponse<QuestionSummaryResponse> popular(
            @RequestParam(defaultValue = "WEEK") PopularPeriod period,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "6") @Min(1) @Max(50) int size) {
        return questionService.popular(period, page, size);
    }

    @Operation(summary = "내 임시 보관함")
    @GetMapping("/drafts")
    public List<QuestionSummaryResponse> myDrafts(@AuthenticationPrincipal AuthUser authUser) {
        return questionService.getMyDrafts(authUser);
    }

    @Operation(summary = "질문 상세", description = "조회수가 기록된다. 같은 사용자는 24시간에 한 번만 센다.")
    @GetMapping("/{questionId}")
    public QuestionDetailResponse detail(@PathVariable Long questionId,
                                         @AuthenticationPrincipal AuthUser authUser,
                                         HttpServletRequest request) {
        return questionService.getDetail(questionId, authUser, viewerKey(authUser, request));
    }

    @Operation(summary = "질문 작성", description = "draft=true면 임시 보관. 응답은 생성된 질문 id")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long create(@Valid @RequestBody QuestionRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return questionService.create(request, authUser);
    }

    @Operation(summary = "질문 수정", description = "작성자만. 임시 보관 질문을 draft=false로 수정하면 등록된다.")
    @PutMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long questionId, @Valid @RequestBody QuestionRequest request,
                       @AuthenticationPrincipal AuthUser authUser) {
        questionService.update(questionId, request, authUser);
    }

    @Operation(summary = "질문 삭제", description = "작성자 또는 관리자")
    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long questionId, @AuthenticationPrincipal AuthUser authUser) {
        questionService.delete(questionId, authUser);
    }

    // 비로그인은 IP+User-Agent 해시로 구분한다 (원문은 저장하지 않음)
    private String viewerKey(AuthUser authUser, HttpServletRequest request) {
        if (authUser != null) {
            return "u:" + authUser.id();
        }
        String raw = request.getRemoteAddr() + "|" + request.getHeader(HttpHeaders.USER_AGENT);
        return "g:" + TokenHasher.sha256(raw).substring(0, 40);
    }
}
