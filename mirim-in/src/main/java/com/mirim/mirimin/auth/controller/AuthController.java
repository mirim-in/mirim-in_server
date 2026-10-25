package com.mirim.mirimin.auth.controller;

import com.mirim.mirimin.auth.dto.EmailRequest;
import com.mirim.mirimin.auth.dto.LoginRequest;
import com.mirim.mirimin.auth.dto.PasswordResetRequest;
import com.mirim.mirimin.auth.dto.RefreshTokenRequest;
import com.mirim.mirimin.auth.dto.SignupRequest;
import com.mirim.mirimin.auth.dto.TokenResponse;
import com.mirim.mirimin.auth.dto.VerifyCodeRequest;
import com.mirim.mirimin.auth.service.AuthService;
import com.mirim.mirimin.auth.service.EmailVerificationService;
import com.mirim.mirimin.auth.service.PasswordResetService;
import com.mirim.mirimin.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final TokenService tokenService;

    @Operation(summary = "가입 인증 코드 발송", description = "6자리 코드를 메일로 보낸다. 같은 이메일은 60초에 한 번.")
    @PostMapping("/email/code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendSignupCode(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.sendSignupCode(AuthService.normalizeEmail(request.email()));
    }

    @Operation(summary = "가입 인증 코드 확인", description = "확인 후 1시간 안에 회원가입을 완료해야 한다. 5회 틀리면 코드를 다시 받아야 한다.")
    @PostMapping("/email/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifySignupCode(@Valid @RequestBody VerifyCodeRequest request) {
        emailVerificationService.verifySignupCode(AuthService.normalizeEmail(request.email()), request.code());
    }

    @Operation(summary = "회원가입", description = "@e-mirim.hs.kr 이메일은 재학생/교직원으로 자동 인증된다. "
            + "EMAIL_VERIFICATION_REQUIRED=true면 이메일 인증 코드 확인을 먼저 마쳐야 한다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "이메일 로그인")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "토큰 재발급", description = "사용한 Refresh Token은 폐기되고 새 토큰 쌍을 발급한다.")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return tokenService.reissue(request.refreshToken());
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 폐기한다. Access Token은 만료될 때까지 유효하니 프론트에서 지워야 한다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        tokenService.revoke(request.refreshToken());
    }

    @Operation(summary = "비밀번호 재설정 메일 요청", description = "가입 여부와 관계없이 항상 204를 응답한다.")
    @PostMapping("/password/reset-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @Operation(summary = "비밀번호 재설정", description = "성공하면 모든 기기에서 로그아웃된다.")
    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }
}
