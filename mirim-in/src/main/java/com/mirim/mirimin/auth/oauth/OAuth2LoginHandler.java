package com.mirim.mirimin.auth.oauth;

import com.mirim.mirimin.auth.dto.TokenResponse;
import com.mirim.mirimin.auth.service.AuthService;
import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Google 로그인 결과를 프론트로 돌려보낸다.
 * 성공: {FRONTEND_URL}/oauth/callback#accessToken=...&refreshToken=...&expiresIn=...
 * 실패: {FRONTEND_URL}/oauth/callback#error={에러 코드}
 * 토큰을 # 뒤(fragment)에 붙여 서버 접근 로그나 Referer에 남지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final String CALLBACK_PATH = "/oauth/callback";

    private final AuthService authService;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        clearSession(request);

        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            redirectError(response, ErrorCode.INVALID_CREDENTIALS);
            return;
        }
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }

        try {
            TokenResponse tokens = authService.loginWithGoogle(email, name, googleId);
            response.sendRedirect(callbackUrl()
                    + "#accessToken=" + encode(tokens.accessToken())
                    + "&refreshToken=" + encode(tokens.refreshToken())
                    + "&expiresIn=" + tokens.accessTokenExpiresIn());
        } catch (BusinessException e) {
            redirectError(response, e.getErrorCode());
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Google 로그인 실패: {}", exception.getMessage());
        clearSession(request);
        redirectError(response, ErrorCode.INVALID_CREDENTIALS);
    }

    private void redirectError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.sendRedirect(callbackUrl() + "#error=" + errorCode.getCode());
    }

    // OAuth 인증 요청(state)을 담느라 만든 세션은 로그인이 끝나면 필요 없다
    private void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private String callbackUrl() {
        return appProperties.frontendUrl() + CALLBACK_PATH;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
