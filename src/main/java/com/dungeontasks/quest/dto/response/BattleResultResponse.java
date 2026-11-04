package com.dungeontasks.quest.dto.response;

import com.dungeontasks.character.dto.response.CharacterResponse;
import java.util.List;
import java.util.UUID;

public record BattleResultResponse(
        UUID questID,
        int xpEarned,
        int goldEarned,
        int levelBefore,
        int levelAfter,
        boolean levelUp,
        MonsterResponse monster,
        List<Integer> hits,
        CharacterResponse character
) {
}