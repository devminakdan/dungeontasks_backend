package com.dungeontasks.quest.dto.response;

import com.dungeontasks.quest.domain.QuestCategory;
import com.dungeontasks.quest.domain.QuestDifficulty;
import com.dungeontasks.quest.domain.QuestStatus;

import java.time.Instant;
import java.util.UUID;

public record QuestResponse(
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