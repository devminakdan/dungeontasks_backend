package com.dungeontasks.auth.security;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant sessionExpiresAt,
        String refreshTokenHash
) {
}
