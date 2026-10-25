package com.mirim.mirimin.auth.service;

import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.mail.MailService;
import com.mirim.mirimin.user.entity.EmailVerification;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.VerificationPurpose;
import com.mirim.mirimin.user.repository.EmailVerificationRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long LINK_VALID_MINUTES = 30;
    private static final long RESEND_INTERVAL_SECONDS = 60;

    private final UserRepository userRepository;
    private final EmailVerificationRepository verificationRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AppProperties appProperties;
    private final Clock clock;

    /**
     * 가입 여부를 알 수 없도록, 메일을 보낼 수 없는 경우(미가입, Google 계정, 탈퇴)에도 조용히 끝낸다.
     */
    @Transactional
    public void requestReset(String rawEmail) {
        String email = AuthService.normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null && !u.isDeleted())
                .orElse(null);
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean recentlySent = verificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(email, VerificationPurpose.PASSWORD_RESET)
                .filter(last -> last.getCreatedAt().isAfter(now.minusSeconds(RESEND_INTERVAL_SECONDS)))
                .isPresent();
        if (recentlySent) {
            return;
        }

        verificationRepository.deleteAllByEmailAndPurpose(email, VerificationPurpose.PASSWORD_RESET);
        String token = TokenHasher.randomToken();
        verificationRepository.save(EmailVerification.builder()
                .email(email)
                .code(TokenHasher.sha256(token))
                .purpose(VerificationPurpose.PASSWORD_RESET)
                .expiresAt(now.plusMinutes(LINK_VALID_MINUTES))
                .build());

        String link = appProperties.frontendUrl() + "/reset-password?token=" + token;
        mailService.send(email, "[미림iN] 비밀번호 재설정",
                "아래 링크에서 비밀번호를 재설정해 주세요. (" + LINK_VALID_MINUTES + "분 동안 유효)\n" + link
                        + "\n\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
    }

    /** 비밀번호를 바꾸면 모든 기기에서 로그아웃된다. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = TokenHasher.sha256(token);
        EmailVerification verification = verificationRepository
                .findByCodeAndPurpose(tokenHash, VerificationPurpose.PASSWORD_RESET)
                .filter(v -> !v.isVerified())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_TOKEN));
        if (!verification.verify(tokenHash, LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
        }
        User user = userRepository.findByEmail(verification.getEmail())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_TOKEN));

        user.changePassword(passwordEncoder.encode(newPassword));
        tokenService.revokeAll(user.getId());
    }
}
