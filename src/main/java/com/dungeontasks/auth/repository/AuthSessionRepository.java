package com.dungeontasks.auth.repository;

import static com.dungeontasks.common.persistence.DatabaseTables.AUTH_SESSION;
import static com.dungeontasks.common.persistence.DatabaseTables.REFRESH_TOKEN_HASH;
import static com.dungeontasks.common.persistence.DatabaseTables.SESSION_CREATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.SESSION_EXPIRES_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.SESSION_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.SESSION_LAST_ROTATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.SESSION_REVOKED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.SESSION_USER_ID;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.dungeontasks.auth.domain.AuthSession;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class AuthSessionRepository {

    private final DSLContext dsl;

    public AuthSessionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void create(UUID sessionID, UUID userID, String refreshHash, Instant now, Instant expiresAt) {
        dsl.insertInto(AUTH_SESSION)
                .columns(
                        SESSION_ID,
                        SESSION_USER_ID,
                        REFRESH_TOKEN_HASH,
                        SESSION_CREATED_AT,
                        SESSION_EXPIRES_AT,
                        SESSION_LAST_ROTATED_AT
                )
                .values(
                        sessionID,
                        userID,
                        refreshHash,
                        atUTC(now),
                        atUTC(expiresAt),
                        atUTC(now)
                )
                .execute();
    }

    public Optional<AuthSession> findByID(UUID sessionID) {
        Record record = dsl.select(SESSION_ID, SESSION_USER_ID, SESSION_EXPIRES_AT, SESSION_REVOKED_AT)
                .from(AUTH_SESSION)
                .where(SESSION_ID.eq(sessionID))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }
        OffsetDateTime revokedAt = record.get(SESSION_REVOKED_AT);
        return Optional.of(new AuthSession(
                record.get(SESSION_ID),
                record.get(SESSION_USER_ID),
                record.get(SESSION_EXPIRES_AT).toInstant(),
                revokedAt == null ? null : revokedAt.toInstant()
        ));
    }

    public boolean rotate(
            UUID sessionID,
            UUID userID,
            String currentRefreshHash,
            String nextRefreshHash,
            Instant now
    ) {
        int updated = dsl.update(AUTH_SESSION)
                .set(REFRESH_TOKEN_HASH, nextRefreshHash)
                .set(SESSION_LAST_ROTATED_AT, atUTC(now))
                .where(SESSION_ID.eq(sessionID))
                .and(SESSION_USER_ID.eq(userID))
                .and(REFRESH_TOKEN_HASH.eq(currentRefreshHash))
                .and(SESSION_REVOKED_AT.isNull())
                .and(SESSION_EXPIRES_AT.gt(atUTC(now)))
                .execute();

        return updated == 1;
    }

    public boolean revoke(UUID sessionID, UUID userID, Instant now) {
        int updated = dsl.update(AUTH_SESSION)
                .set(SESSION_REVOKED_AT, atUTC(now))
                .where(SESSION_ID.eq(sessionID))
                .and(SESSION_USER_ID.eq(userID))
                .and(SESSION_REVOKED_AT.isNull())
                .execute();

        return updated == 1;
    }

    private static OffsetDateTime atUTC(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
