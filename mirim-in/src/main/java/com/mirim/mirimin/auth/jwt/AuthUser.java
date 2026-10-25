package com.mirim.mirimin.auth.jwt;

import com.mirim.mirimin.user.entity.Role;

/**
 * 인증된 사용자. 컨트롤러에서 @AuthenticationPrincipal AuthUser 로 받는다.
 * 토큰 정보만 담고 있으므로 정지·탈퇴 여부 등은 서비스에서 DB로 확인한다.
 */
public record AuthUser(Long id, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
