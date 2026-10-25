package com.mirim.mirimin.question.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.service.QuestionReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "저도 궁금해요·즐겨찾기")
@RestController
@RequiredArgsConstructor
public class QuestionReactionController {

    private final QuestionReactionService reactionService;

    @Operation(summary = "저도 궁금해요", description = "본인 질문에는 불가. 이미 눌렀으면 아무 일도 없다.")
    @PutMapping("/api/questions/{questionId}/curiosity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addCuriosity(@PathVariable Long questionId, @AuthenticationPrincipal AuthUser authUser) {
        reactionService.addCuriosity(questionId, authUser);
    }

    @Operation(summary = "저도 궁금해요 취소")
    @DeleteMapping("/api/questions/{questionId}/curiosity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCuriosity(@PathVariable Long questionId, @AuthenticationPrincipal AuthUser authUser) {
        reactionService.removeCuriosity(questionId, authUser);
    }

    @Operation(summary = "즐겨찾기 추가")
    @PutMapping("/api/questions/{questionId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addBookmark(@PathVariable Long questionId, @AuthenticationPrincipal AuthUser authUser) {
        reactionService.addBookmark(questionId, authUser);
    }

    @Operation(summary = "즐겨찾기 취소")
    @DeleteMapping("/api/questions/{questionId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@PathVariable Long questionId, @AuthenticationPrincipal AuthUser authUser) {
        reactionService.removeBookmark(questionId, authUser);
    }

    @Operation(summary = "내 즐겨찾기 목록", description = "프로필 > 즐겨찾기 탭. 최근에 담은 순")
    @GetMapping("/api/users/me/bookmarks")
    public PageResponse<QuestionSummaryResponse> myBookmarks(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return reactionService.getMyBookmarks(authUser, page, size);
    }
}
