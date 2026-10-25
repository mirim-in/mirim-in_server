package com.mirim.mirimin.user.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.dto.MeResponse;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "회원")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
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
}
