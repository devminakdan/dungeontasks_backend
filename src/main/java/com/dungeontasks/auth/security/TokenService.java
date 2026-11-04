package com.dungeontasks.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.dungeontasks.auth.config.AuthProperties;
import com.dungeontasks.common.error.InvalidAuthenticationException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String SESSION_ID_CLAIM = "session_id";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final AuthProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public TokenService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SecretKey key = new SecretKeySpec(
                properties.jwtSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                new JwtAudienceValidator(properties.audience()),
                expirationRequiredValidator()
        ));
        this.decoder = jwtDecoder;
    }

    public TokenPair issue(UUID userID, UUID sessionID, Instant sessionExpiresAt) {
        Instant now = clock.instant();
        Instant accessExpiresAt = now.plus(properties.accessTokenTtl());
        if (accessExpiresAt.isAfter(sessionExpiresAt)) {
            accessExpiresAt = sessionExpiresAt;
        }
        if (!accessExpiresAt.isAfter(now)) {
            throw new InvalidAuthenticationException("The session has expired");
        }

        String accessToken = encode(userID, sessionID, ACCESS_TYPE, now, accessExpiresAt, UUID.randomUUID());
        String refreshToken = encode(
                userID,
                sessionID,
                REFRESH_TYPE,
                now,
                sessionExpiresAt,
                UUID.randomUUID()
        );
        return new TokenPair(
                accessToken,
                refreshToken,
                accessExpiresAt,
                sessionExpiresAt,
                hash(refreshToken)
        );
    }

    public Jwt decodeAccessToken(String token) {
        return decode(token, ACCESS_TYPE);
    }

    public Jwt decodeRefreshToken(String token) {
        return decode(token, REFRESH_TYPE);
    }

    public UUID userID(Jwt jwt) {
        return parseUUID(jwt.getSubject());
    }

    public UUID sessionID(Jwt jwt) {
        return parseUUID(jwt.getClaimAsString(SESSION_ID_CLAIM));
    }

    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String encode(
            UUID userID,
            UUID sessionID,
            String type,
            Instant issuedAt,
            Instant expiresAt,
            UUID tokenID
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(java.util.List.of(properties.audience()))
                .subject(userID.toString())
                .id(tokenID.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(TOKEN_TYPE_CLAIM, type)
                .claim(SESSION_ID_CLAIM, sessionID.toString())
                .build();
        try {
            JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
            return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        } catch (JwtEncodingException exception) {
            throw new IllegalStateException("Unable to issue authentication token", exception);
        }
    }

    private Jwt decode(String token, String expectedType) {
        try {
            Jwt jwt = decoder.decode(token);
            if (!expectedType.equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM))) {
                throw new InvalidAuthenticationException("The token type is invalid");
            }
            userID(jwt);
            sessionID(jwt);
            return jwt;
        } catch (InvalidAuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new InvalidAuthenticationException("The authentication token is invalid or expired");
        }
    }

    private UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new InvalidAuthenticationException("The authentication token contains invalid identifiers");
        }
    }

    private OAuth2TokenValidator<Jwt> expirationRequiredValidator() {
        return jwt -> {
            if (jwt.getExpiresAt() != null) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The token expiry is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
