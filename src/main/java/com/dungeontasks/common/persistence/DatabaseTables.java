package com.dungeontasks.common.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class DatabaseTables {
    public static final Table<?> APP_USER = DSL.table(DSL.name("app_user"));
    public static final Field<UUID> USER_ID = DSL.field(DSL.name("app_user", "user_id"), SQLDataType.UUID);
    public static final Field<String> USERNAME = DSL.field(DSL.name("app_user", "username"), SQLDataType.VARCHAR);
    public static final Field<OffsetDateTime> USER_CREATED_AT = DSL.field(
            DSL.name("app_user", "created_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> USER_UPDATED_AT = DSL.field(
            DSL.name("app_user", "updated_at"), SQLDataType.OFFSETDATETIME);

    public static final Table<?> EXTERNAL_IDENTITY = DSL.table(DSL.name("external_identity"));
    public static final Field<UUID> IDENTITY_ID = DSL.field(
            DSL.name("external_identity", "identity_id"), SQLDataType.UUID);
    public static final Field<UUID> IDENTITY_USER_ID = DSL.field(
            DSL.name("external_identity", "user_id"), SQLDataType.UUID);
    public static final Field<String> IDENTITY_PROVIDER = DSL.field(
            DSL.name("external_identity", "provider"), SQLDataType.VARCHAR);
    public static final Field<String> IDENTITY_SUBJECT = DSL.field(
            DSL.name("external_identity", "provider_subject"), SQLDataType.VARCHAR);
    public static final Field<String> IDENTITY_EMAIL = DSL.field(
            DSL.name("external_identity", "email"), SQLDataType.VARCHAR);
    public static final Field<String> IDENTITY_AVATAR_URL = DSL.field(
            DSL.name("external_identity", "avatar_url"), SQLDataType.CLOB);
    public static final Field<OffsetDateTime> IDENTITY_CREATED_AT = DSL.field(
            DSL.name("external_identity", "created_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> IDENTITY_UPDATED_AT = DSL.field(
            DSL.name("external_identity", "updated_at"), SQLDataType.OFFSETDATETIME);

    public static final Table<?> AUTH_SESSION = DSL.table(DSL.name("auth_session"));
    public static final Field<UUID> SESSION_ID = DSL.field(
            DSL.name("auth_session", "session_id"), SQLDataType.UUID);
    public static final Field<UUID> SESSION_USER_ID = DSL.field(
            DSL.name("auth_session", "user_id"), SQLDataType.UUID);
    public static final Field<String> REFRESH_TOKEN_HASH = DSL.field(
            DSL.name("auth_session", "refresh_token_hash"), SQLDataType.CHAR(64));
    public static final Field<OffsetDateTime> SESSION_CREATED_AT = DSL.field(
            DSL.name("auth_session", "created_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> SESSION_EXPIRES_AT = DSL.field(
            DSL.name("auth_session", "expires_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> SESSION_LAST_ROTATED_AT = DSL.field(
            DSL.name("auth_session", "last_rotated_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> SESSION_REVOKED_AT = DSL.field(
            DSL.name("auth_session", "revoked_at"), SQLDataType.OFFSETDATETIME);

    public static final Table<?> QUEST = DSL.table(DSL.name("quest"));
    public static final Field<UUID> QUEST_ID = DSL.field(DSL.name("quest", "quest_id"), SQLDataType.UUID);
    public static final Field<UUID> QUEST_USER_ID = DSL.field(DSL.name("quest", "user_id"), SQLDataType.UUID);
    public static final Field<String> QUEST_TITLE = DSL.field(DSL.name("quest", "title"), SQLDataType.VARCHAR);
    public static final Field<String> QUEST_DESCRIPTION = DSL.field(DSL.name("quest", "description"), SQLDataType.CLOB);
    public static final Field<String> QUEST_DIFFICULTY = DSL.field(DSL.name("quest", "difficulty"), SQLDataType.VARCHAR);
    public static final Field<String> QUEST_CATEGORY = DSL.field(DSL.name("quest", "category"), SQLDataType.VARCHAR);
    public static final Field<OffsetDateTime> QUEST_DEADLINE = DSL.field(
            DSL.name("quest", "deadline"), SQLDataType.OFFSETDATETIME);
    public static final Field<String> QUEST_STATUS = DSL.field(DSL.name("quest", "status"), SQLDataType.VARCHAR);
    public static final Field<OffsetDateTime> QUEST_CREATED_AT = DSL.field(
            DSL.name("quest", "created_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> QUEST_UPDATED_AT = DSL.field(
            DSL.name("quest", "updated_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> QUEST_COMPLETED_AT = DSL.field(
            DSL.name("quest", "completed_at"), SQLDataType.OFFSETDATETIME);

    public static final Table<?> CHARACTER = DSL.table(DSL.name("character"));
    public static final Field<UUID> CHARACTER_ID = DSL.field(DSL.name("character", "character_id"), SQLDataType.UUID);
    public static final Field<UUID> CHARACTER_USER_ID = DSL.field(DSL.name("character", "user_id"), SQLDataType.UUID);
    public static final Field<Integer> CHARACTER_LEVEL = DSL.field(DSL.name("character", "level"), SQLDataType.INTEGER);
    public static final Field<Long> CHARACTER_XP = DSL.field(DSL.name("character", "xp"), SQLDataType.BIGINT);
    public static final Field<Long> CHARACTER_GOLD = DSL.field(DSL.name("character", "gold"), SQLDataType.BIGINT);
    public static final Field<Integer> CHARACTER_ATTACK = DSL.field(DSL.name("character", "attack"), SQLDataType.INTEGER);
    public static final Field<Integer> CHARACTER_DEFENSE = DSL.field(DSL.name("character", "defense"), SQLDataType.INTEGER);
    public static final Field<Integer> CHARACTER_LUCK = DSL.field(DSL.name("character", "luck"), SQLDataType.INTEGER);
    public static final Field<OffsetDateTime> CHARACTER_CREATED_AT = DSL.field(
            DSL.name("character", "created_at"), SQLDataType.OFFSETDATETIME);
    public static final Field<OffsetDateTime> CHARACTER_UPDATED_AT = DSL.field(
            DSL.name("character", "updated_at"), SQLDataType.OFFSETDATETIME);

    public static final Table<?> QUEST_COMPLETION = DSL.table(DSL.name("quest_completion"));
    public static final Field<UUID> COMPLETION_ID = DSL.field(DSL.name("quest_completion", "completion_id"), SQLDataType.UUID);
    public static final Field<UUID> COMPLETION_QUEST_ID = DSL.field(DSL.name("quest_completion", "quest_id"), SQLDataType.UUID);
    public static final Field<UUID> COMPLETION_USER_ID = DSL.field(DSL.name("quest_completion", "user_id"), SQLDataType.UUID);
    public static final Field<Integer> COMPLETION_XP_EARNED = DSL.field(DSL.name("quest_completion", "xp_earned"), SQLDataType.INTEGER);
    public static final Field<Integer> COMPLETION_GOLD_EARNED = DSL.field(DSL.name("quest_completion", "gold_earned"), SQLDataType.INTEGER);
    public static final Field<Integer> COMPLETION_LEVEL_BEFORE = DSL.field(DSL.name("quest_completion", "level_before"), SQLDataType.INTEGER);
    public static final Field<Integer> COMPLETION_LEVEL_AFTER = DSL.field(DSL.name("quest_completion", "level_after"), SQLDataType.INTEGER);
    public static final Field<String> COMPLETION_MONSTER_TYPE = DSL.field(DSL.name("quest_completion", "monster_type"), SQLDataType.VARCHAR);
    public static final Field<Integer> COMPLETION_MONSTER_MAX_HP = DSL.field(DSL.name("quest_completion", "monster_max_hp"), SQLDataType.INTEGER);
    public static final Field<String> COMPLETION_HITS_JSON = DSL.field(DSL.name("quest_completion", "hits_json"), SQLDataType.CLOB);
    public static final Field<OffsetDateTime> COMPLETION_COMPLETED_AT = DSL.field(
            DSL.name("quest_completion", "completed_at"), SQLDataType.OFFSETDATETIME);

    private DatabaseTables() {
    }
}
