package com.dungeontasks.character.domain;

import java.time.Instant;
import java.util.UUID;

public record Character(
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