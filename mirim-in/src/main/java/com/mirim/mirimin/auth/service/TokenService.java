package com.mirim.mirimin.auth.service;

import com.mirim.mirimin.auth.dto.TokenResponse;
import com.mirim.mirimin.auth.entity.RefreshToken;
import com.mirim.mirimin.auth.jwt.JwtProvider;
import com.mirim.mirimin.auth.repository.RefreshTokenRepository;
import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;
    private final Clock clock;

    @Transactional
    public TokenResponse issue(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = TokenHasher.randomToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock)
                .plusNanos(appProperties.jwt().refreshTokenExpiration() * 1_000_000);
        refreshTokenRepository.save(new RefreshToken(user, TokenHasher.sha256(refreshToken), expiresAt));
        return new TokenResponse(accessToken, refreshToken, jwtProvider.getAccessTokenExpirationSeconds());
    }

    /** 쓴 Refresh Token은 지우고 새로 발급한다. */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse reissue(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        refreshTokenRepository.delete(saved);

        User user = saved.getUser();
        if (saved.isExpired(LocalDateTime.now(clock)) || user.isDeleted()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return issue(user);
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(refreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
