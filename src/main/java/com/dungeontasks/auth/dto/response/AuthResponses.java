package com.dungeontasks.auth.dto.response;

import java.time.Instant;

import com.dungeontasks.auth.security.TokenPair;
import com.dungeontasks.auth.service.AuthResult;
import com.dungeontasks.user.dto.response.UserResponses;
import io.swagger.v3.oas.annotations.media.Schema;

public final class AuthResponses {

    private AuthResponses() {
    }

    @Schema(name = "AuthSession", description = "DungeonTasks token pair and current profile")
    public record AuthResponse(
            @Schema(
                    description = "Short-lived DungeonTasks bearer JWT",
                    example = "eyJhbGciOiJIUzI1NiJ9...",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String accessToken,
            @Schema(
                    description = "Single-use DungeonTasks refresh JWT",
                    example = "eyJhbGciOiJIUzI1NiJ9...",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String refreshToken,
            @Schema(
                    description = "Access-token expiry timestamp, clamped to the absolute session expiry",
                    example = "2026-10-13T12:15:00Z",
                    format = "date-time",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Instant accessTokenExpiresAt,
            @Schema(
                    description = "Absolute session expiry, 30 days after Google login and unchanged by refresh",
                    example = "2026-11-12T12:00:00Z",
                    format = "date-time",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Instant sessionExpiresAt,
            @Schema(description = "Current user profile", requiredMode = Schema.RequiredMode.REQUIRED)
            UserResponses.UserProfileResponse user
    ) {
    }

    public static AuthResponse from(AuthResult result) {
        TokenPair tokens = result.tokenPair();
        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresAt(),
                tokens.sessionExpiresAt(),
                UserResponses.from(result.profile())
        );
    }
}
