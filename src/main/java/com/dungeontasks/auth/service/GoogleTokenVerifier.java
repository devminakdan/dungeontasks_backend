package com.dungeontasks.auth.service;

import java.util.Set;

import com.dungeontasks.auth.config.AuthProperties;
import com.dungeontasks.auth.domain.GoogleIdentity;
import com.dungeontasks.common.error.AuthenticationUnavailableException;
import com.dungeontasks.common.error.InvalidAuthenticationException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifier {

    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );

    private final AuthProperties properties;
    private volatile JwtDecoder decoder;

    public GoogleTokenVerifier(AuthProperties properties) {
        this.properties = properties;
    }

    public GoogleIdentity verify(String identityToken) {
        if (!properties.isGoogleConfigured()) {
            throw new AuthenticationUnavailableException("Google Sign-In is not configured on the server");
        }

        Jwt jwt;
        try {
            jwt = decoder().decode(identityToken);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAuthenticationException("The Google identity token is invalid or expired");
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        if (subject == null || subject.isBlank() || !isValidEmail(email)
                || !Boolean.TRUE.equals(emailVerified)) {
            throw new InvalidAuthenticationException("The Google identity token does not contain a verified email");
        }

        return new GoogleIdentity(subject, email, jwt.getClaimAsString("picture"));
    }

    private JwtDecoder decoder() {
        JwtDecoder current = decoder;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (decoder == null) {
                NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(properties.googleJwkSetUri())
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
                jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(),
                        new JwtAudienceValidator(properties.googleClientID()),
                        expirationRequiredValidator(),
                        issuerValidator()
                ));
                decoder = jwtDecoder;
            }
            return decoder;
        }
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            if (GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The Google issuer is invalid", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    private OAuth2TokenValidator<Jwt> expirationRequiredValidator() {
        return jwt -> {
            if (jwt.getExpiresAt() != null) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The Google token expiry is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int separator = email.indexOf('@');
        return separator > 0 && separator < email.length() - 1 && separator == email.lastIndexOf('@');
    }
}
