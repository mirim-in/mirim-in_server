package com.mirim.mirimin.user.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.image.controller.ImageController.ImageUploadResponse;
import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.user.dto.ProfileResponse;
import com.mirim.mirimin.user.dto.ProfileUpdateRequest;
import com.mirim.mirimin.user.dto.TechStacksRequest;
import com.mirim.mirimin.user.dto.UserAnswerResponse;
import com.mirim.mirimin.user.dto.UserCategoriesRequest;
import com.mirim.mirimin.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "프로필")
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "프로필", description = "누구나 볼 수 있다. email은 본인 프로필에서만 채워진다.")
    @GetMapping("/api/users/{userId}/profile")
    public ProfileResponse getProfile(@PathVariable Long userId, @AuthenticationPrincipal AuthUser authUser) {
        return profileService.getProfile(userId, authUser);
    }

    @Operation(summary = "프로필 > 질문 목록", description = "최근 활동 탭. 본인이 아니면 익명 질문은 빠진다.")
    @GetMapping("/api/users/{userId}/questions")
    public PageResponse<QuestionSummaryResponse> getQuestions(
            @PathVariable Long userId, @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return profileService.getQuestions(userId, authUser, page, size);
    }

    @Operation(summary = "프로필 > 답변 목록", description = "최근 활동 탭")
    @GetMapping("/api/users/{userId}/answers")
    public PageResponse<UserAnswerResponse> getAnswers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return profileService.getAnswers(userId, page, size);
    }

    @Operation(summary = "내 기본 정보 수정", description = "이름, 반, 자기소개, 학과(재학생은 학과 변경 불가)")
    @PutMapping("/api/users/me/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                              @AuthenticationPrincipal AuthUser authUser) {
        profileService.updateProfile(request, authUser);
    }

    @Operation(summary = "관심 분야·답변 가능 분야 설정",
            description = "공식 카테고리 id. 보낸 목록으로 통째로 바뀐다. 답변 가능 분야는 학교 구성원만")
    @PutMapping("/api/users/me/categories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCategories(@Valid @RequestBody UserCategoriesRequest request,
                                 @AuthenticationPrincipal AuthUser authUser) {
        profileService.updateCategories(request, authUser);
    }

    @Operation(summary = "기술 스택 설정", description = "보낸 목록으로 통째로 바뀐다. 최대 20개")
    @PutMapping("/api/users/me/tech-stacks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTechStacks(@Valid @RequestBody TechStacksRequest request,
                                 @AuthenticationPrincipal AuthUser authUser) {
        profileService.updateTechStacks(request, authUser);
    }

    @Operation(summary = "프로필 사진 변경", description = "jpg/png/gif/webp, 5MB 이하")
    @PutMapping(value = "/api/users/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse updateProfileImage(@RequestPart("file") MultipartFile file,
                                                  @AuthenticationPrincipal AuthUser authUser) {
        return new ImageUploadResponse(profileService.updateProfileImage(file, authUser));
    }

    @Operation(summary = "프로필 사진 삭제")
    @DeleteMapping("/api/users/me/profile-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfileImage(@AuthenticationPrincipal AuthUser authUser) {
        profileService.deleteProfileImage(authUser);
    }
}
