package com.dungeontasks.quest.domain;

import java.time.Instant;
import java.util.UUID;

public record Quest(
        UUID questID,
        UUID userID,
        String title,
        String description,
        QuestDifficulty difficulty,
        QuestCategory category,
        Instant deadline,
        QuestStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}