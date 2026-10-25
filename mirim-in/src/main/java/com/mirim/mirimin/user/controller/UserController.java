package com.mirim.mirimin.user.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.dto.MeResponse;
import com.mirim.mirimin.user.dto.VerificationRequestCreateRequest;
import com.mirim.mirimin.user.dto.VerificationRequestResponse;
import com.mirim.mirimin.user.dto.WithdrawRequest;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.service.MemberVerificationService;
import com.mirim.mirimin.user.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "회원")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final MemberVerificationService memberVerificationService;
    private final WithdrawalService withdrawalService;
    private final Clock clock;

    @Operation(summary = "내 정보", description = "로그인한 사용자의 기본 정보. 헤더의 이름·포인트·인증 배지 표시에 쓴다.")
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal AuthUser authUser) {
        User user = userRepository.findById(authUser.id())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return MeResponse.of(user, LocalDate.now(clock), user.isWriteRestricted(LocalDateTime.now(clock)));
    }

    @Operation(summary = "미림인 승인 신청",
            description = "학교 이메일이 없는 사용자(졸업생 등). 심사 중인 신청은 하나만. 응답은 신청 id")
    @PostMapping("/me/verification-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Long requestVerification(@Valid @RequestBody VerificationRequestCreateRequest request,
                                    @AuthenticationPrincipal AuthUser authUser) {
        return memberVerificationService.request(request, authUser);
    }

    @Operation(summary = "내 미림인 승인 신청 내역", description = "최근 신청 순")
    @GetMapping("/me/verification-requests")
    public List<VerificationRequestResponse> myVerificationRequests(@AuthenticationPrincipal AuthUser authUser) {
        return memberVerificationService.getMyRequests(authUser);
    }

    @Operation(summary = "회원 탈퇴",
            description = "이메일 가입자는 password 필요. 개인정보는 지워지고 작성한 글은 '탈퇴한 사용자'로 남는다.")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@RequestBody(required = false) WithdrawRequest request,
                         @AuthenticationPrincipal AuthUser authUser) {
        withdrawalService.withdrawMe(request == null ? null : request.password(), authUser);
    }
}
