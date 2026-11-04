package com.dungeontasks.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        UUID sessionID,
        UUID userID,
        Instant expiresAt,
        Instant revokedAt
) {
    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }
}
