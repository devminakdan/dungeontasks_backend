package com.dungeontasks.quest.repository;

import static com.dungeontasks.common.persistence.DatabaseTables.QUEST;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_CATEGORY;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_COMPLETED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_CREATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_DEADLINE;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_DESCRIPTION;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_DIFFICULTY;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_STATUS;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_TITLE;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_UPDATED_AT;
import static com.dungeontasks.common.persistence.DatabaseTables.QUEST_USER_ID;

import com.dungeontasks.quest.domain.Quest;
import com.dungeontasks.quest.domain.QuestCategory;
import com.dungeontasks.quest.domain.QuestDifficulty;
import com.dungeontasks.quest.domain.QuestStatus;
import com.dungeontasks.quest.dto.request.CreateQuestRequest;
import com.dungeontasks.quest.dto.request.UpdateQuestRequest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
public class QuestRepository {

    private final DSLContext dsl;

    public QuestRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Quest insertQuest(UUID userID, CreateQuestRequest request, Instant now) {
        UUID questID = UUID.randomUUID();
        OffsetDateTime timestamp = atUTC(now);
        OffsetDateTime deadline = request.deadline() == null ? null : atUTC(request.deadline());

        dsl.insertInto(QUEST)
                .columns(
                        QUEST_ID,
                        QUEST_USER_ID,
                        QUEST_TITLE,
                        QUEST_DESCRIPTION,
                        QUEST_DIFFICULTY,
                        QUEST_CATEGORY,
                        QUEST_DEADLINE,
                        QUEST_STATUS,
                        QUEST_CREATED_AT,
                        QUEST_UPDATED_AT,
                        QUEST_COMPLETED_AT
                )
                .values(
                        questID,
                        userID,
                        request.title(),
                        request.description(),
                        request.difficulty().name(),
                        request.category().name(),
                        deadline,
                        QuestStatus.ACTIVE.name(),
                        timestamp,
                        timestamp,
                        null
                )
                .execute();

        return new Quest(
                questID,
                userID,
                request.title(),
                request.description(),
                request.difficulty(),
                request.category(),
                request.deadline(),
                QuestStatus.ACTIVE,
                now,
                now,
                null
        );
    }

    public Optional<Quest> findById(UUID questID) {
        Record record = dsl.select(
                        QUEST_ID, QUEST_USER_ID, QUEST_TITLE, QUEST_DESCRIPTION,
                        QUEST_DIFFICULTY, QUEST_CATEGORY, QUEST_DEADLINE, QUEST_STATUS,
                        QUEST_CREATED_AT, QUEST_UPDATED_AT, QUEST_COMPLETED_AT
                )
                .from(QUEST)
                .where(QUEST_ID.eq(questID))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(map(record));
    }

    public List<Quest> findByUser(UUID userID, String status, String category, String difficulty, int page, int size) {
        var select = dsl.select(
                        QUEST_ID, QUEST_USER_ID, QUEST_TITLE, QUEST_DESCRIPTION,
                        QUEST_DIFFICULTY, QUEST_CATEGORY, QUEST_DEADLINE, QUEST_STATUS,
                        QUEST_CREATED_AT, QUEST_UPDATED_AT, QUEST_COMPLETED_AT
                )
                .from(QUEST)
                .where(QUEST_USER_ID.eq(userID));

        if (status != null && !status.isBlank()) {
            select = select.and(QUEST_STATUS.eq(status));
        }
        if (category != null && !category.isBlank()) {
            select = select.and(QUEST_CATEGORY.eq(category));
        }
        if (difficulty != null && !difficulty.isBlank()) {
            select = select.and(QUEST_DIFFICULTY.eq(difficulty));
        }

        return select
                .orderBy(QUEST_CREATED_AT.desc(), QUEST_ID.desc())
                .limit(size)
                .offset((long) page * size)
                .fetch()
                .map(this::map);
    }

    public long countByUser(UUID userID, String status, String category, String difficulty) {
        var select = dsl.selectCount()
                .from(QUEST)
                .where(QUEST_USER_ID.eq(userID));

        if (status != null && !status.isBlank()) {
            select = select.and(QUEST_STATUS.eq(status));
        }
        if (category != null && !category.isBlank()) {
            select = select.and(QUEST_CATEGORY.eq(category));
        }
        if (difficulty != null && !difficulty.isBlank()) {
            select = select.and(QUEST_DIFFICULTY.eq(difficulty));
        }

        return select.fetchOne(0, Long.class);
    }

    public void updateQuest(UUID questID, UpdateQuestRequest request, Instant now) {
        OffsetDateTime timestamp = atUTC(now);
        var update = dsl.update(QUEST).set(QUEST_UPDATED_AT, timestamp);

        if (request.title() != null) {
            update = update.set(QUEST_TITLE, request.title());
        }
        if (request.description() != null) {
            update = update.set(QUEST_DESCRIPTION, request.description());
        }
        if (request.difficulty() != null) {
            update = update.set(QUEST_DIFFICULTY, request.difficulty().name());
        }
        if (request.category() != null) {
            update = update.set(QUEST_CATEGORY, request.category().name());
        }
        if (request.deadline() != null) {
            update = update.set(QUEST_DEADLINE, atUTC(request.deadline()));
        }
        if (request.status() != null) {
            update = update.set(QUEST_STATUS, request.status().name());
            if (request.status() == QuestStatus.COMPLETED) {
                update = update.set(QUEST_COMPLETED_AT, timestamp);
            }
            if (request.status() == QuestStatus.ACTIVE || request.status() == QuestStatus.CANCELLED) {
                update = update.set(QUEST_COMPLETED_AT, (OffsetDateTime) null);
            }
        }

        update.where(QUEST_ID.eq(questID)).execute();
    }

    public void deleteQuest(UUID questID) {
        dsl.deleteFrom(QUEST)
                .where(QUEST_ID.eq(questID))
                .execute();
    }

    private Quest map(Record record) {
        OffsetDateTime deadline = record.get(QUEST_DEADLINE);
        OffsetDateTime completedAt = record.get(QUEST_COMPLETED_AT);
        return new Quest(
                record.get(QUEST_ID),
                record.get(QUEST_USER_ID),
                record.get(QUEST_TITLE),
                record.get(QUEST_DESCRIPTION),
                QuestDifficulty.valueOf(record.get(QUEST_DIFFICULTY)),
                QuestCategory.valueOf(record.get(QUEST_CATEGORY)),
                deadline == null ? null : deadline.toInstant(),
                QuestStatus.valueOf(record.get(QUEST_STATUS)),
                record.get(QUEST_CREATED_AT).toInstant(),
                record.get(QUEST_UPDATED_AT).toInstant(),
                completedAt == null ? null : completedAt.toInstant()
        );
    }

    private static OffsetDateTime atUTC(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}