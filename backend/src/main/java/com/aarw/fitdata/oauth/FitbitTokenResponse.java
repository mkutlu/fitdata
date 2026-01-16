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

    public static FitbitTokenResponse fromMap(java.util.Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        String access = String.valueOf(body.get("access_token"));
        String refresh = String.valueOf(body.get("refresh_token"));
        String type = body.get("token_type") == null ? null : String.valueOf(body.get("token_type"));
        String scope = body.get("scope") == null ? null : String.valueOf(body.get("scope"));
        String userId = body.get("user_id") == null ? null : String.valueOf(body.get("user_id"));
        long expiresIn = body.get("expires_in") == null ? 0L : Long.parseLong(String.valueOf(body.get("expires_in")));

        return new FitbitTokenResponse(access, refresh, type, scope, userId, expiresIn);
    }
}