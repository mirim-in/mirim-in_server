package com.mirim.mirimin.auth.service;

import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.mail.MailService;
import com.mirim.mirimin.user.entity.EmailVerification;
import com.mirim.mirimin.user.entity.VerificationPurpose;
import com.mirim.mirimin.user.repository.EmailVerificationRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** 가입 인증 코드 발송·확인 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final long RESEND_INTERVAL_SECONDS = 60;
    // 코드 확인 후 이 시간 안에 가입을 끝내야 한다
    private static final long VERIFIED_VALID_MINUTES = 60;

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final AppProperties appProperties;
    private final Clock clock;

    @Transactional
    public void sendSignupCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        verificationRepository.findTopByEmailAndPurposeOrderByIdDesc(email, VerificationPurpose.SIGNUP)
                .filter(last -> last.getCreatedAt().isAfter(now.minusSeconds(RESEND_INTERVAL_SECONDS)))
                .ifPresent(last -> {
                    throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
                });

        verificationRepository.deleteAllByEmailAndPurpose(email, VerificationPurpose.SIGNUP);
        String code = TokenHasher.randomDigits(CODE_LENGTH);
        long expiresInSeconds = appProperties.auth().emailCodeExpiration();
        verificationRepository.save(EmailVerification.builder()
                .email(email)
                .code(code)
                .purpose(VerificationPurpose.SIGNUP)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .build());

        mailService.send(email, "[미림iN] 이메일 인증 코드",
                "미림iN 이메일 인증 코드는 [" + code + "] 입니다.\n"
                        + (expiresInSeconds / 60) + "분 안에 입력해 주세요.");
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void verifySignupCode(String email, String code) {
        EmailVerification verification = verificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(email, VerificationPurpose.SIGNUP)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE));
        if (verification.isLocked()) {
            throw new BusinessException(ErrorCode.VERIFICATION_LOCKED);
        }
        // 실패해도 시도 횟수는 저장돼야 하므로 noRollbackFor
        if (!verification.verify(code, LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
    }

    @Transactional(readOnly = true)
    public void assertSignupVerified(String email) {
        LocalDateTime validFrom = LocalDateTime.now(clock).minusMinutes(VERIFIED_VALID_MINUTES);
        boolean verified = verificationRepository
                .findTopByEmailAndPurposeOrderByIdDesc(email, VerificationPurpose.SIGNUP)
                .filter(EmailVerification::isVerified)
                .filter(v -> v.getCreatedAt().isAfter(validFrom))
                .isPresent();
        if (!verified) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    @Transactional
    public void consumeSignup(String email) {
        verificationRepository.deleteAllByEmailAndPurpose(email, VerificationPurpose.SIGNUP);
    }
}
