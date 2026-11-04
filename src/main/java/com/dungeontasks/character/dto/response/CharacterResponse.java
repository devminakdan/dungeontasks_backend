package com.dungeontasks.character.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CharacterResponse(
        UUID characterID,
        UUID userID,
        int level,
        long xp,
        long xpToNextLevel,
        long gold,
        int attack,
        int defense,
        int luck,
        Instant createdAt,
        Instant updatedAt
) {
}