package com.dungeontasks.inventory.dto.response;

import com.dungeontasks.inventory.domain.ItemRarity;
import com.dungeontasks.inventory.domain.ItemType;

import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID userItemID,
        UUID itemID,
        String name,
        String description,
        ItemRarity rarity,
        ItemType type,
        int attackBonus,
        int defenseBonus,
        int luckBonus,
        boolean isEquipped,
        Instant obtainedAt
) {
}