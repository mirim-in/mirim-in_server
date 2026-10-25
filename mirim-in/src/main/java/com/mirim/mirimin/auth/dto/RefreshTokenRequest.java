package com.mirim.mirimin.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token이 필요해요.")
        String refreshToken
) {
}
