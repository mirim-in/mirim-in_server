package com.mirim.mirimin.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.properties 의 app.* 설정 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Jwt jwt,
        Auth auth,
        Cloudinary cloudinary,
        Admin admin
) {

    public record Admin(String email, String password, String name) {
    }

    public record Jwt(String secret, long accessTokenExpiration, long refreshTokenExpiration) {
    }

    public record Auth(String schoolEmailDomain, long emailCodeExpiration) {
    }

    public record Cloudinary(String cloudName, String apiKey, String apiSecret, String folder) {
    }
}
