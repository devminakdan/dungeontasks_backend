package com.dungeontasks.auth.service;

import java.time.Instant;
import java.util.UUID;

import com.dungeontasks.auth.repository.AuthSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionMutationService {

    private final AuthSessionRepository sessionRepository;

    public SessionMutationService(AuthSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean rotate(
            UUID sessionID,
            UUID userID,
            String currentRefreshHash,
            String nextRefreshHash,
            Instant now
    ) {
        return sessionRepository.rotate(sessionID, userID, currentRefreshHash, nextRefreshHash, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(UUID sessionID, UUID userID, Instant now) {
        sessionRepository.revoke(sessionID, userID, now);
    }
}
