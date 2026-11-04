package com.dungeontasks.inventory.service;

import com.dungeontasks.inventory.domain.ItemDefinition;
import com.dungeontasks.inventory.domain.ItemRarity;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class LootDropService {

    private final Random random = new Random();

    public boolean shouldDrop(String difficulty) {
        double chance = switch (difficulty) {
            case "EASY" -> 0.30;
            case "MEDIUM" -> 0.50;
            case "HARD" -> 0.80;
            case "EPIC" -> 1.0;
            default -> 0.0;
        };
        return random.nextDouble() < chance;
    }

    public ItemRarity rollRarity(String difficulty) {
        double roll = random.nextDouble();
        // cumulative
        double legendary = 0.01;
        double epic = 0.06;
        double rare = 0.13;
        double uncommon = 0.25;
        // common remainder
        boolean minUncommon = "EPIC".equals(difficulty);
        if (roll < legendary && !minUncommon) return ItemRarity.LEGENDARY;
        if (roll < legendary + epic) return ItemRarity.EPIC;
        if (roll < legendary + epic + rare) return ItemRarity.RARE;
        if (roll < legendary + epic + rare + uncommon) return ItemRarity.UNCOMMON;
        if (minUncommon) return ItemRarity.UNCOMMON;
        return ItemRarity.COMMON;
    }

    public ItemDefinition pickItem(ItemRarity rarity, List<ItemDefinition> definitions) {
        List<ItemDefinition> candidates = definitions.stream()
                .filter(d -> d.rarity() == rarity)
                .toList();
        if (candidates.isEmpty()) {
            candidates = definitions;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}