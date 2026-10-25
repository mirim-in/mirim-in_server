package com.mirim.mirimin.auth.service;

import com.mirim.mirimin.auth.dto.LoginRequest;
import com.mirim.mirimin.auth.dto.SignupRequest;
import com.mirim.mirimin.auth.dto.TokenResponse;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.entity.AuthProvider;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final SchoolEmailClassifier schoolEmailClassifier;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        emailVerificationService.assertSignupVerified(email);

        User user = userRepository.save(newUser(email, request.name().trim(), AuthProvider.LOCAL, null,
                passwordEncoder.encode(request.password())));
        emailVerificationService.consumeSignup(email);
        return tokenService.issue(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }
        return tokenService.issue(user);
    }

    /**
     * Google이 이메일 소유를 확인해 주므로 별도 인증 코드 없이 가입·로그인시킨다.
     * 같은 이메일로 이미 이메일 가입한 계정이 있으면 그 계정으로 로그인된다.
     */
    @Transactional
    public TokenResponse loginWithGoogle(String email, String name, String googleId) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized)
                .orElseGet(() -> userRepository.save(newUser(normalized, name, AuthProvider.GOOGLE, googleId, null)));
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }
        return tokenService.issue(user);
    }

    private User newUser(String email, String name, AuthProvider provider, String providerId, String passwordHash) {
        SchoolEmailClassifier.Classification c = schoolEmailClassifier.classify(email);
        return User.builder()
                .email(email)
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .passwordHash(passwordHash)
                .memberType(c.memberType())
                .department(c.department())
                .admissionYear(c.admissionYear())
                .build();
    }
}
