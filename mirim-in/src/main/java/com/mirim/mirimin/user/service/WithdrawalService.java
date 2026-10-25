package com.mirim.mirimin.user.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.auth.service.TokenService;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserCategoryType;
import com.mirim.mirimin.user.repository.UserCategoryRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final UserReader userReader;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /** 이메일 가입자는 비밀번호를 확인한다. 관리자 강제 탈퇴는 withdraw(User)를 직접 쓴다. */
    @Transactional
    public void withdrawMe(String password, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        if (user.getPasswordHash() != null
                && (password == null || !passwordEncoder.matches(password, user.getPasswordHash()))) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "비밀번호가 올바르지 않아요.");
        }
        withdraw(user);
    }

    /**
     * 개인정보(이메일, 이름, 사진, 소개, 분야, 기술 스택)를 지우고 모든 기기에서 로그아웃시킨다.
     * 작성한 질문·답변·댓글은 "탈퇴한 사용자"로 남는다.
     */
    @Transactional
    public void withdraw(User user) {
        Long userId = user.getId();
        tokenService.revokeAll(userId);
        userCategoryRepository.deleteByUserIdAndType(userId, UserCategoryType.INTEREST);
        userCategoryRepository.deleteByUserIdAndType(userId, UserCategoryType.ANSWERABLE);
        userTechStackRepository.deleteByUserId(userId);
        // 위의 삭제 쿼리가 영속성 컨텍스트를 비우므로 다시 읽어서 수정한다
        userRepository.findById(userId).orElseThrow().withdraw(LocalDateTime.now(clock));
    }
}
