package com.dungeontasks.user.repository;

import static com.dungeontasks.common.persistence.DatabaseTables.APP_USER;
import static com.dungeontasks.common.persistence.DatabaseTables.EXTERNAL_IDENTITY;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_AVATAR_URL;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_CREATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_EMAIL;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_PROVIDER;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_SUBJECT;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_UPDATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.IDENTITY_USER_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.USERNAME;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_CREATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_UPDATED_AT;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.dungeontasks.auth.domain.GoogleIdentity;
import com.dungeontasks.user.domain.UserProfile;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String GOOGLE_PROVIDER = "google";

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UUID> findUserIDByGoogleSubject(String subject) {
        return dsl.select(IDENTITY_USER_ID)
                .from(EXTERNAL_IDENTITY)
                .where(IDENTITY_PROVIDER.eq(GOOGLE_PROVIDER))
                .and(IDENTITY_SUBJECT.eq(subject))
                .fetchOptional(IDENTITY_USER_ID);
    }

    public void createUser(UUID userID, Instant now) {
        OffsetDateTime timestamp = atUTC(now);
        dsl.insertInto(APP_USER)
                .columns(USER_ID, USER_CREATED_AT, USER_UPDATED_AT)
                .values(userID, timestamp, timestamp)
                .execute();
    }

    public boolean createGoogleIdentity(UUID userID, GoogleIdentity identity, Instant now) {
        OffsetDateTime timestamp = atUTC(now);
        int inserted = dsl.insertInto(EXTERNAL_IDENTITY)
                .columns(
                        IDENTITY_ID,
                        IDENTITY_USER_ID,
                        IDENTITY_PROVIDER,
                        IDENTITY_SUBJECT,
                        IDENTITY_EMAIL,
                        IDENTITY_AVATAR_URL,
                        IDENTITY_CREATED_AT,
                        IDENTITY_UPDATED_AT
                )
                .values(
                        UUID.randomUUID(),
                        userID,
                        GOOGLE_PROVIDER,
                        identity.subject(),
                        identity.email(),
                        identity.avatarURL(),
                        timestamp,
                        timestamp
                )
                .onConflict(IDENTITY_PROVIDER, IDENTITY_SUBJECT)
                .doNothing()
                .execute();

        return inserted == 1;
    }

    public void deleteUser(UUID userID) {
        dsl.deleteFrom(APP_USER)
                .where(USER_ID.eq(userID))
                .execute();
    }

    public void updateGoogleIdentity(UUID userID, GoogleIdentity identity, Instant now) {
        dsl.update(EXTERNAL_IDENTITY)
                .set(IDENTITY_EMAIL, identity.email())
                .set(IDENTITY_AVATAR_URL, identity.avatarURL())
                .set(IDENTITY_UPDATED_AT, atUTC(now))
                .where(IDENTITY_USER_ID.eq(userID))
                .and(IDENTITY_PROVIDER.eq(GOOGLE_PROVIDER))
                .execute();
    }

    public Optional<UserProfile> findProfileByUserID(UUID userID) {
        Record record = dsl.select(USER_ID, USERNAME, IDENTITY_EMAIL, IDENTITY_AVATAR_URL)
                .from(APP_USER)
                .leftJoin(EXTERNAL_IDENTITY)
                .on(IDENTITY_USER_ID.eq(USER_ID))
                .and(IDENTITY_PROVIDER.eq(GOOGLE_PROVIDER))
                .where(USER_ID.eq(userID))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(new UserProfile(
                record.get(USER_ID),
                record.get(USERNAME),
                record.get(IDENTITY_EMAIL),
                record.get(IDENTITY_AVATAR_URL)
        ));
    }

    public void updateUsername(UUID userID, String username, Instant now) {
        dsl.update(APP_USER)
                .set(USERNAME, username)
                .set(USER_UPDATED_AT, atUTC(now))
                .where(USER_ID.eq(userID))
                .execute();
    }

    public boolean existsByUsername(String username) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(APP_USER)
                        .where(USERNAME.eq(username))
        );
    }

    private static OffsetDateTime atUTC(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
