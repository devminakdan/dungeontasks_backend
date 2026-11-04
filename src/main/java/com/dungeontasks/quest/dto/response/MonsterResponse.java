package com.dungeontasks.quest.dto.response;

public record MonsterResponse(
        String name,
        String type,
        int maxHp
) {
}