package com.dungeontasks.quest.repository;

import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_COMPLETED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_LOOT_USER_ITEM_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_GOLD_EARNED;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_HITS_JSON;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_LEVEL_AFTER;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_LEVEL_BEFORE;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_MONSTER_MAX_HP;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_MONSTER_TYPE;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_QUEST_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_USER_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.COMPLETION_XP_EARNED;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_COMPLETION;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class QuestCompletionRepository {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestCompletionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record StoredCompletion(
            UUID questID,
            UUID userID,
            int xpEarned,
            int goldEarned,
            int levelBefore,
            int levelAfter,
            String monsterType,
            int monsterMaxHp,
            List<Integer> hits,
            UUID lootUserItemID
    ) {
    }

    public Optional<StoredCompletion> findByQuestID(UUID questID) {
        Record record = dsl.select(
                        COMPLETION_QUEST_ID, COMPLETION_USER_ID, COMPLETION_XP_EARNED, COMPLETION_GOLD_EARNED,
                        COMPLETION_LEVEL_BEFORE, COMPLETION_LEVEL_AFTER, COMPLETION_MONSTER_TYPE,
                        COMPLETION_MONSTER_MAX_HP, COMPLETION_HITS_JSON, COMPLETION_LOOT_USER_ITEM_ID
                )
                .from(QUEST_COMPLETION)
                .where(COMPLETION_QUEST_ID.eq(questID))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        List<Integer> hits;
        try {
            hits = objectMapper.readValue(record.get(COMPLETION_HITS_JSON), new TypeReference<>() {
            });
        } catch (Exception e) {
            hits = List.of();
        }

        return Optional.of(new StoredCompletion(
                record.get(COMPLETION_QUEST_ID),
                record.get(COMPLETION_USER_ID),
                record.get(COMPLETION_XP_EARNED),
                record.get(COMPLETION_GOLD_EARNED),
                record.get(COMPLETION_LEVEL_BEFORE),
                record.get(COMPLETION_LEVEL_AFTER),
                record.get(COMPLETION_MONSTER_TYPE),
                record.get(COMPLETION_MONSTER_MAX_HP),
                hits,
                record.get(COMPLETION_LOOT_USER_ITEM_ID)
        ));
    }

    public void insert(
            UUID questID,
            UUID userID,
            int xpEarned,
            int goldEarned,
            int levelBefore,
            int levelAfter,
            String monsterType,
            int monsterMaxHp,
            List<Integer> hits,
            UUID lootUserItemID,
            Instant now
    ) {
        String hitsJson;
        try {
            hitsJson = objectMapper.writeValueAsString(hits);
        } catch (Exception e) {
            hitsJson = "[]";
        }

        dsl.insertInto(QUEST_COMPLETION)
                .columns(
                        COMPLETION_ID, COMPLETION_QUEST_ID, COMPLETION_USER_ID, COMPLETION_XP_EARNED,
                        COMPLETION_GOLD_EARNED, COMPLETION_LEVEL_BEFORE, COMPLETION_LEVEL_AFTER,
                        COMPLETION_MONSTER_TYPE, COMPLETION_MONSTER_MAX_HP, COMPLETION_HITS_JSON,
                        COMPLETION_LOOT_USER_ITEM_ID, COMPLETION_COMPLETED_AT
                )
                .values(
                        UUID.randomUUID(), questID, userID, xpEarned,
                        goldEarned, levelBefore, levelAfter,
                        monsterType, monsterMaxHp, hitsJson,
                        lootUserItemID, OffsetDateTime.now(ZoneOffset.UTC)
                )
                .execute();
    }
}