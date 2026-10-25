package com.mirim.mirimin.auth.dto;

/** accessTokenExpiresIn: 초 단위 */
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
