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

    private DatabaseTables() {
    }
}
