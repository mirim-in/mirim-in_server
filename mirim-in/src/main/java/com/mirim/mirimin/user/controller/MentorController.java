package com.mirim.mirimin.user.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.user.dto.MentorResponse;
import com.mirim.mirimin.user.dto.MentorUpdateRequest;
import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.service.MentorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "멘토")
@RestController
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    @Operation(summary = "멘토 목록",
            description = "멘토를 켠 사용자. categoryId는 답변 가능 분야, keyword는 이름·소개에서 찾는다.")
    @GetMapping("/api/mentors")
    public PageResponse<MentorResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") MentorService.MentorSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return mentorService.search(categoryId, department, keyword, sort, page, size);
    }

    @Operation(summary = "멘토 전환·소개 수정",
            description = "enabled=true는 2학년 이상 재학생, 졸업생, 교직원만. intro를 보내지 않으면 기존 소개를 유지한다.")
    @PutMapping("/api/users/me/mentor")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@Valid @RequestBody MentorUpdateRequest request, @AuthenticationPrincipal AuthUser authUser) {
        mentorService.update(request, authUser);
    }
}
