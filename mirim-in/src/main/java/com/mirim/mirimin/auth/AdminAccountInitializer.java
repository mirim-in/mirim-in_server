package com.mirim.mirimin.auth;

import com.mirim.mirimin.auth.service.AuthService;
import com.mirim.mirimin.auth.service.SchoolEmailClassifier;
import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.user.entity.AuthProvider;
import com.mirim.mirimin.user.entity.Role;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버 시작 시 ADMIN_EMAIL 계정을 관리자로 만든다.
 * - 계정이 없으면 ADMIN_PASSWORD로 새로 만든다.
 * - 이미 있으면 권한만 ADMIN으로 올린다. (비밀번호는 바꾸지 않는다)
 * ADMIN_EMAIL이 비어 있으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final SchoolEmailClassifier schoolEmailClassifier;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = appProperties.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()) {
            return;
        }
        String email = AuthService.normalizeEmail(admin.email());

        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            if (!user.isAdmin()) {
                user.promoteToAdmin();
                log.info("관리자 권한 부여: {}", email);
            }
        }, () -> {
            if (admin.password() == null || admin.password().isBlank()) {
                log.warn("ADMIN_PASSWORD가 비어 있어 관리자 계정을 만들지 않았어요: {}", email);
                return;
            }
            SchoolEmailClassifier.Classification c = schoolEmailClassifier.classify(email);
            userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(admin.password()))
                    .provider(AuthProvider.LOCAL)
                    .name(admin.name() == null || admin.name().isBlank() ? "관리자" : admin.name())
                    .role(Role.ADMIN)
                    .memberType(c.memberType())
                    .department(c.department())
                    .admissionYear(c.admissionYear())
                    .build());
            log.info("관리자 계정 생성: {}", email);
        });
    }
}
