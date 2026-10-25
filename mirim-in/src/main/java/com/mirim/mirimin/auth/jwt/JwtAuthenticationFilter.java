package com.mirim.mirimin.auth.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer {accessToken} 을 읽어 인증 정보를 채운다.
 * 토큰이 잘못됐으면 인증 없이 통과시키고, 보호된 API라면 SecurityErrorHandler가 A002로 응답한다.
 * SecurityConfig에서 직접 생성한다. (@Component로 만들면 서블릿 필터로 한 번 더 등록된다)
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String INVALID_TOKEN_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".INVALID";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                AuthUser authUser = jwtProvider.parse(header.substring(BEARER_PREFIX.length()));
                var authentication = new UsernamePasswordAuthenticationToken(authUser, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + authUser.role().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(INVALID_TOKEN_ATTRIBUTE, true);
            }
        }
        chain.doFilter(request, response);
    }
}
