package com.aarw.fitdata.oauth;

import java.time.Instant;

public record FitbitTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String scope,
        String userId,
        long expiresIn
) {
    public Instant expiresAt() {
        return Instant.now().plusSeconds(expiresIn);
    }
}