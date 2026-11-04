package com.dungeontasks.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {
    }

    @Schema(name = "GoogleLoginRequest", description = "Google ID token exchange request")
    public record GoogleLoginRequest(
            @NotBlank
            @Size(min = 1, max = 16384)
            @Schema(
                    description = "Non-blank RS256 Google ID token issued for the configured Web OAuth client",
                    example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ii4uLiJ9...",
                    minLength = 1,
                    maxLength = 16384
            )
            String identityToken
    ) {
    }

    @Schema(name = "RefreshTokenRequest", description = "DungeonTasks refresh-token rotation request")
    public record RefreshTokenRequest(
            @NotBlank
            @Size(min = 1, max = 8192)
            @Schema(
                    description = "Non-blank current DungeonTasks refresh JWT. A token can be used only once",
                    example = "eyJhbGciOiJIUzI1NiJ9...",
                    minLength = 1,
                    maxLength = 8192
            )
            String refreshToken
    ) {
    }
}
