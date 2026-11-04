package com.dungeontasks.inventory.domain;

import java.util.UUID;

public record ItemDefinition(
        UUID itemID,
        String name,
        String description,
        ItemRarity rarity,
        ItemType type,
        int attackBonus,
        int defenseBonus,
        int luckBonus
) {
}