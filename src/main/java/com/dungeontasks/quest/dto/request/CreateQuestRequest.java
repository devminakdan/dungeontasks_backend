package com.dungeontasks.quest.dto.request;

import com.dungeontasks.quest.domain.QuestCategory;
import com.dungeontasks.quest.domain.QuestDifficulty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateQuestRequest(
        @NotBlank @Size(min = 1, max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull QuestDifficulty difficulty,
        @NotNull QuestCategory category,
        @Future Instant deadline
) {
}