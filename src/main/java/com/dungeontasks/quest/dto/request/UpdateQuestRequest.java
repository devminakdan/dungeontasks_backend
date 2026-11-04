package com.dungeontasks.quest.dto.request;

import com.dungeontasks.quest.domain.QuestCategory;
import com.dungeontasks.quest.domain.QuestDifficulty;
import com.dungeontasks.quest.domain.QuestStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateQuestRequest(
        @Size(min = 1, max = 120) String title,
        @Size(max = 1000) String description,
        QuestDifficulty difficulty,
        QuestCategory category,
        @Future Instant deadline,
        QuestStatus status
) {
}