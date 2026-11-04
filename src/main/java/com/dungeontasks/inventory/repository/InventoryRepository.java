package com.dungeontasks.inventory.repository;

import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_ATTACK_BONUS;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_DEFENSE_BONUS;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_DESCRIPTION;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_LUCK_BONUS;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_NAME;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_RARITY;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_TYPE;
import static com.dungeontasks.common.persistence.DatabaseTables.ITEM_DEFINITION;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ITEM;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ITEM_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ITEM_USER_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ITEM_ITEM_ID;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ITEM_IS_EQUIPPED;
import static com.dungeontasks.common.persistence.DatabaseTables.USER_ITEM_OBTAINED_AT;

import com.dungeontasks.inventory.domain.ItemDefinition;
import com.dungeontasks.inventory.domain.ItemRarity;
import com.dungeontasks.inventory.domain.ItemType;
import com.dungeontasks.inventory.domain.UserItem;
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
public class InventoryRepository {

    private final DSLContext dsl;

    public InventoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<ItemDefinition> findItemDefinition(UUID itemID) {
        Record record = dsl.select(
                        ITEM_ID, ITEM_NAME, ITEM_DESCRIPTION, ITEM_RARITY, ITEM_TYPE,
                        ITEM_ATTACK_BONUS, ITEM_DEFENSE_BONUS, ITEM_LUCK_BONUS
                )
                .from(ITEM_DEFINITION)
                .where(ITEM_ID.eq(itemID))
                .fetchOne();
        if (record == null) return Optional.empty();
        return Optional.of(mapDefinition(record));
    }

    public List<ItemDefinition> findAllDefinitions() {
        return dsl.select(
                        ITEM_ID, ITEM_NAME, ITEM_DESCRIPTION, ITEM_RARITY, ITEM_TYPE,
                        ITEM_ATTACK_BONUS, ITEM_DEFENSE_BONUS, ITEM_LUCK_BONUS
                )
                .from(ITEM_DEFINITION)
                .fetch()
                .map(this::mapDefinition);
    }

    public UserItem insertUserItem(UUID userID, UUID itemID, Instant now) {
        UUID userItemID = UUID.randomUUID();
        OffsetDateTime timestamp = now.atOffset(ZoneOffset.UTC);
        dsl.insertInto(USER_ITEM)
                .columns(USER_ITEM_ID, USER_ITEM_USER_ID, USER_ITEM_ITEM_ID, USER_ITEM_IS_EQUIPPED, USER_ITEM_OBTAINED_AT)
                .values(userItemID, userID, itemID, false, timestamp)
                .execute();
        return findUserItem(userItemID).orElseThrow();
    }

    public List<UserItem> findByUser(UUID userID) {
        return dsl.select(
                        USER_ITEM_ID, USER_ITEM_USER_ID, USER_ITEM_ITEM_ID, USER_ITEM_IS_EQUIPPED, USER_ITEM_OBTAINED_AT
                )
                .from(USER_ITEM)
                .where(USER_ITEM_USER_ID.eq(userID))
                .orderBy(USER_ITEM_OBTAINED_AT.desc())
                .fetch()
                .map(this::mapUserItem);
    }

    public Optional<UserItem> findUserItem(UUID userItemID) {
        Record record = dsl.select(
                        USER_ITEM_ID, USER_ITEM_USER_ID, USER_ITEM_ITEM_ID, USER_ITEM_IS_EQUIPPED, USER_ITEM_OBTAINED_AT
                )
                .from(USER_ITEM)
                .where(USER_ITEM_ID.eq(userItemID))
                .fetchOne();
        if (record == null) return Optional.empty();
        return Optional.of(mapUserItem(record));
    }

    private ItemDefinition mapDefinition(Record record) {
        return new ItemDefinition(
                record.get(ITEM_ID),
                record.get(ITEM_NAME),
                record.get(ITEM_DESCRIPTION),
                ItemRarity.valueOf(record.get(ITEM_RARITY)),
                ItemType.valueOf(record.get(ITEM_TYPE)),
                record.get(ITEM_ATTACK_BONUS),
                record.get(ITEM_DEFENSE_BONUS),
                record.get(ITEM_LUCK_BONUS)
        );
    }

    private UserItem mapUserItem(Record record) {
        UUID itemID = record.get(USER_ITEM_ITEM_ID);
        ItemDefinition def = findItemDefinition(itemID).orElseThrow();
        return new UserItem(
                record.get(USER_ITEM_ID),
                record.get(USER_ITEM_USER_ID),
                def,
                record.get(USER_ITEM_IS_EQUIPPED),
                record.get(USER_ITEM_OBTAINED_AT).toInstant()
        );
    }
}