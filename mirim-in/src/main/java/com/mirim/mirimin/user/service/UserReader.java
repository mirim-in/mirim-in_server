package com.mirim.mirimin.user.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/** 토큰의 사용자 정보를 DB 상태(탈퇴, 정지)까지 확인해 가져온다. */
@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;
    private final Clock clock;

    /** 로그인 필수. 공개 API에서 로그인한 경우에만 쓸 수 있는 동작에 사용한다. */
    public User getActiveUser(AuthUser authUser) {
        if (authUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findById(authUser.id())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /** 글쓰기(질문, 답변, 댓글 등). 정지 중이면 거부한다. */
    public User getWritableUser(AuthUser authUser) {
        User user = getActiveUser(authUser);
        if (user.isWriteRestricted(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.WRITE_RESTRICTED);
        }
        return user;
    }
}
