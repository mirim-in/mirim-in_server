package com.mirim.mirimin.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfig와 분리한다.
 * (SecurityConfig → OAuth2LoginHandler → AuthService → PasswordEncoder 순환 참조 방지)
 */
@Configuration
public class PasswordEncoderConfig {

    /** bcrypt. 저장 형식이 "{bcrypt}..." 라서 나중에 알고리즘을 바꿔도 기존 해시를 읽을 수 있다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
