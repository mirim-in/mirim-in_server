package com.mirim.mirimin.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 현재 시각은 LocalDateTime.now(clock)으로 얻는다.
 * 테스트에서 Clock을 고정해 학년 계산, 정지 기간, 인증 코드 만료를 검증할 수 있다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SEOUL);
    }
}
