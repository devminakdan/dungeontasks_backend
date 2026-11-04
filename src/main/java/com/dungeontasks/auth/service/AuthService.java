package com.dungeontasks.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.dungeontasks.auth.config.AuthProperties;
import com.dungeontasks.auth.domain.AuthSession;
import com.dungeontasks.auth.domain.GoogleIdentity;
import com.dungeontasks.auth.repository.AuthSessionRepository;
import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.auth.security.TokenPair;
import com.dungeontasks.auth.security.TokenService;
import com.dungeontasks.common.error.InvalidAuthenticationException;
import com.dungeontasks.common.error.ResourceNotFoundException;
import com.dungeontasks.user.domain.UserProfile;
import com.dungeontasks.user.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final TokenService tokenService;
    private final AuthSessionRepository sessionRepository;
    private final SessionMutationService sessionMutationService;
    private final UserRepository userRepository;
    private final AuthProperties properties;
    private final Clock clock;

    public AuthService(
            GoogleTokenVerifier googleTokenVerifier,
            TokenService tokenService,
            AuthSessionRepository sessionRepository,
            SessionMutationService sessionMutationService,
            UserRepository userRepository,
            AuthProperties properties,
            Clock clock
    ) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.sessionMutationService = sessionMutationService;
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthResult loginWithGoogle(String identityToken) {
        GoogleIdentity identity = googleTokenVerifier.verify(identityToken);
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        UUID userID = findOrCreateUser(identity, now);
        userRepository.updateGoogleIdentity(userID, identity, now);

        UUID sessionID = UUID.randomUUID();
        Instant sessionExpiresAt = now.plus(properties.sessionTtl());
        TokenPair tokens = tokenService.issue(userID, sessionID, sessionExpiresAt);
        sessionRepository.create(sessionID, userID, tokens.refreshTokenHash(), now, sessionExpiresAt);

        return new AuthResult(tokens, requireProfile(userID));
    }

    public AuthResult refresh(String refreshToken) {
        Jwt jwt = tokenService.decodeRefreshToken(refreshToken);
        UUID userID = tokenService.userID(jwt);
        UUID sessionID = tokenService.sessionID(jwt);
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        AuthSession session = sessionRepository.findByID(sessionID)
                .filter(candidate -> candidate.userID().equals(userID))
                .filter(candidate -> candidate.isActiveAt(now))
                .orElseThrow(() -> new InvalidAuthenticationException("The session is invalid or expired"));

        TokenPair nextTokens = tokenService.issue(userID, sessionID, session.expiresAt());
        boolean rotated = sessionMutationService.rotate(
                sessionID,
                userID,
                tokenService.hash(refreshToken),
                nextTokens.refreshTokenHash(),
                now
        );
        if (!rotated) {
            sessionMutationService.revoke(sessionID, userID, now);
            throw new InvalidAuthenticationException(
                    "Refresh-token reuse was detected and the device session was revoked"
            );
        }

        return new AuthResult(nextTokens, requireProfile(userID));
    }

    public void logout(CurrentUser currentUser) {
        sessionMutationService.revoke(currentUser.sessionID(), currentUser.userID(), clock.instant());
    }

    private UUID findOrCreateUser(GoogleIdentity identity, Instant now) {
        UUID existingUserID = userRepository.findUserIDByGoogleSubject(identity.subject()).orElse(null);
        if (existingUserID != null) {
            return existingUserID;
        }

        UUID newUserID = UUID.randomUUID();
        userRepository.createUser(newUserID, now);
        if (userRepository.createGoogleIdentity(newUserID, identity, now)) {
            return newUserID;
        }

        userRepository.deleteUser(newUserID);
        return userRepository.findUserIDByGoogleSubject(identity.subject())
                .orElseThrow(() -> new IllegalStateException("Concurrent identity creation did not resolve"));
    }

    private UserProfile requireProfile(UUID userID) {
        return userRepository.findProfileByUserID(userID)
                .orElseThrow(() -> new ResourceNotFoundException("The authenticated user no longer exists"));
    }
}
