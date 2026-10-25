package com.mirim.mirimin.auth.jwt;

import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final JwtParser parser;
    private final long accessTokenExpirationMs;
    private final Clock clock;

    public JwtProvider(AppProperties appProperties, Clock clock) {
        String secret = appProperties.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET 환경변수를 32바이트 이상으로 설정해 주세요. (예: openssl rand -base64 32)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
        this.accessTokenExpirationMs = appProperties.jwt().accessTokenExpiration();
        this.clock = clock;
    }

    public String createAccessToken(Long userId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(key)
                .compact();
    }

    /** 서명·만료가 올바르지 않으면 JwtException */
    public AuthUser parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        try {
            return new AuthUser(Long.valueOf(claims.getSubject()), Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
        } catch (RuntimeException e) {
            throw new JwtException("토큰 내용이 올바르지 않음", e);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
