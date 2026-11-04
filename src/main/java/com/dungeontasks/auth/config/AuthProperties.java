package com.dungeontasks.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties("app.auth")
public class AuthProperties {

    private final String issuer;
    private final String audience;
    private final String jwtSecret;
    private final Duration accessTokenTtl;
    private final Duration sessionTtl;
    private final String googleClientID;
    private final String googleJwkSetUri;

    public AuthProperties(
            String issuer,
            String audience,
            String jwtSecret,
            Duration accessTokenTtl,
            Duration sessionTtl,
            @Name("google-client-id") String googleClientID,
            String googleJwkSetUri
    ) {
        this.issuer = requireText(issuer, "JWT issuer");
        this.audience = requireText(audience, "JWT audience");
        this.jwtSecret = validateSecret(jwtSecret);
        this.accessTokenTtl = requirePositive(accessTokenTtl, "Access token TTL");
        this.sessionTtl = requirePositive(sessionTtl, "Session TTL");
        this.googleClientID = googleClientID == null ? "" : googleClientID.trim();
        this.googleJwkSetUri = requireText(googleJwkSetUri, "Google JWK set URI");
    }

    public String issuer() {
        return issuer;
    }

    public String audience() {
        return audience;
    }

    public String jwtSecret() {
        return jwtSecret;
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    public Duration sessionTtl() {
        return sessionTtl;
    }

    public String googleClientID() {
        return googleClientID;
    }

    public String googleJwkSetUri() {
        return googleJwkSetUri;
    }

    public boolean isGoogleConfigured() {
        return !googleClientID.isBlank();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured");
        }
        return value.trim();
    }

    private static String validateSecret(String value) {
        String secret = requireText(value, "JWT secret");
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        return secret;
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(name + " must be positive");
        }
        return value;
    }
}
