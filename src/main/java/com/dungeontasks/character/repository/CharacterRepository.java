package com.dungeontasks.character.repository;

import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_ATTACK;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_CREATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_DEFENSE;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_GOLD;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_LEVEL;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_LUCK;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_UPDATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_USER_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.CHARACTER_XP;

import com.dungeontasks.character.domain.Character;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class CharacterRepository {

    private final DSLContext dsl;

    public CharacterRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<Character> findByUserID(UUID userID) {
        Record record = dsl.select(
                        CHARACTER_ID, CHARACTER_USER_ID, CHARACTER_LEVEL, CHARACTER_XP, CHARACTER_GOLD,
                        CHARACTER_ATTACK, CHARACTER_DEFENSE, CHARACTER_LUCK,
                        CHARACTER_CREATED_AT, CHARACTER_UPDATED_AT
                )
                .from(CHARACTER)
                .where(CHARACTER_USER_ID.eq(userID))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(map(record));
    }

    public Character createCharacter(UUID userID, Instant now) {
        UUID characterID = UUID.randomUUID();
        OffsetDateTime timestamp = atUTC(now);
        dsl.insertInto(CHARACTER)
                .columns(
                        CHARACTER_ID, CHARACTER_USER_ID, CHARACTER_LEVEL, CHARACTER_XP, CHARACTER_GOLD,
                        CHARACTER_ATTACK, CHARACTER_DEFENSE, CHARACTER_LUCK,
                        CHARACTER_CREATED_AT, CHARACTER_UPDATED_AT
                )
                .values(characterID, userID, 1, 0L, 0L, 10, 5, 5, timestamp, timestamp)
                .execute();
        return new Character(characterID, userID, 1, 0L, xpRequired(1), 0L, 10, 5, 5, now, now);
    }

    public Character updateCharacter(Character character, Instant now) {
        OffsetDateTime timestamp = atUTC(now);
        dsl.update(CHARACTER)
                .set(CHARACTER_LEVEL, character.level())
                .set(CHARACTER_XP, character.xp())
                .set(CHARACTER_GOLD, character.gold())
                .set(CHARACTER_ATTACK, character.attack())
                .set(CHARACTER_DEFENSE, character.defense())
                .set(CHARACTER_LUCK, character.luck())
                .set(CHARACTER_UPDATED_AT, timestamp)
                .where(CHARACTER_USER_ID.eq(character.userID()))
                .execute();
        return new Character(
                character.characterID(),
                character.userID(),
                character.level(),
                character.xp(),
                xpRequired(character.level()),
                character.gold(),
                character.attack(),
                character.defense(),
                character.luck(),
                character.createdAt(),
                now
        );
    }

    private Character map(Record record) {
        int level = record.get(CHARACTER_LEVEL);
        return new Character(
                record.get(CHARACTER_ID),
                record.get(CHARACTER_USER_ID),
                level,
                record.get(CHARACTER_XP),
                xpRequired(level),
                record.get(CHARACTER_GOLD),
                record.get(CHARACTER_ATTACK),
                record.get(CHARACTER_DEFENSE),
                record.get(CHARACTER_LUCK),
                record.get(CHARACTER_CREATED_AT).toInstant(),
                record.get(CHARACTER_UPDATED_AT).toInstant()
        );
    }

    public static long xpRequired(int level) {
        return (long) Math.floor(100 * Math.pow(level, 1.5));
    }

    private static OffsetDateTime atUTC(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}