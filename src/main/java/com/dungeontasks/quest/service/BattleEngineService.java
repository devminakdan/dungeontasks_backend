package com.dungeontasks.quest.service;

import com.dungeontasks.character.domain.Character;
import com.dungeontasks.character.dto.response.CharacterResponse;
import com.dungeontasks.character.repository.CharacterRepository;
import com.dungeontasks.character.service.CharacterService;
import com.dungeontasks.common.error.ApiException;
import com.dungeontasks.common.error.ForbiddenException;
import com.dungeontasks.common.error.ResourceNotFoundException;
import com.dungeontasks.quest.domain.Quest;
import com.dungeontasks.quest.domain.QuestStatus;
import com.dungeontasks.quest.dto.response.BattleResultResponse;
import com.dungeontasks.quest.dto.response.MonsterResponse;
import com.dungeontasks.quest.repository.QuestCompletionRepository;
import com.dungeontasks.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleEngineService {

    private final QuestRepository questRepository;
    private final CharacterRepository characterRepository;
    private final QuestCompletionRepository completionRepository;
    private final CharacterService characterService;

    public BattleEngineService(
            QuestRepository questRepository,
            CharacterRepository characterRepository,
            QuestCompletionRepository completionRepository,
            CharacterService characterService
    ) {
        this.questRepository = questRepository;
        this.characterRepository = characterRepository;
        this.completionRepository = completionRepository;
        this.characterService = characterService;
    }

    @Transactional
    public BattleResultResponse complete(UUID currentUserID, UUID questID) {
        Quest quest = questRepository.findById(questID)
                .orElseThrow(() -> new ResourceNotFoundException("Quest with ID '" + questID + "' does not exist"));

        if (!quest.userID().equals(currentUserID)) {
            throw new ForbiddenException("Quest with ID '" + questID + "' belongs to another user");
        }
        if (quest.status() == QuestStatus.CANCELLED || quest.status() == QuestStatus.EXPIRED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bad request", "Quest cannot be completed");
        }

        var existing = completionRepository.findByQuestID(questID);
        if (existing.isPresent()) {
            var completion = existing.get();
            Character character = characterRepository.findByUserID(currentUserID)
                    .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
            CharacterResponse characterResponse = characterService.toResponse(character);
            MonsterResponse monster = new MonsterResponse(
                    monsterName(completion.monsterType()), completion.monsterType(), completion.monsterMaxHp()
            );
            return new BattleResultResponse(
                    questID,
                    completion.xpEarned(),
                    completion.goldEarned(),
                    completion.levelBefore(),
                    completion.levelAfter(),
                    completion.levelAfter() > completion.levelBefore(),
                    monster,
                    completion.hits(),
                    characterResponse
            );
        }

        QuestReward reward = rewardFor(quest.difficulty().name());

        Instant now = Instant.now();
        Character character = characterRepository.findByUserID(currentUserID)
                .orElseGet(() -> characterRepository.createCharacter(currentUserID, now));

        int levelBefore = character.level();
        long xp = character.xp() + reward.xp;
        long gold = character.gold() + reward.gold;
        int level = character.level();
        int attack = character.attack();
        int defense = character.defense();
        int luck = character.luck();

        while (xp >= CharacterRepository.xpRequired(level)) {
            xp -= CharacterRepository.xpRequired(level);
            level++;
            attack += 2;
            defense += 1;
            luck += 1;
        }

        int levelAfter = level;
        boolean levelUp = levelAfter > levelBefore;

        Character updated = new Character(
                character.characterID(),
                character.userID(),
                levelAfter,
                xp,
                CharacterRepository.xpRequired(levelAfter),
                gold,
                attack,
                defense,
                luck,
                character.createdAt(),
                now
        );
        characterRepository.updateCharacter(updated, now);

        questRepository.markCompleted(questID, now);
        completionRepository.insert(
                questID,
                currentUserID,
                reward.xp,
                reward.gold,
                levelBefore,
                levelAfter,
                reward.monsterType,
                reward.maxHp,
                reward.hits,
                now
        );

        CharacterResponse characterResponse = characterService.toResponse(characterRepository.findByUserID(currentUserID).orElse(updated));

        MonsterResponse monster = new MonsterResponse(reward.monsterName, reward.monsterType, reward.maxHp);

        return new BattleResultResponse(
                questID,
                reward.xp,
                reward.gold,
                levelBefore,
                levelAfter,
                levelUp,
                monster,
                reward.hits,
                characterResponse
        );
    }

    private record QuestReward(String monsterName, String monsterType, int maxHp, List<Integer> hits, int xp, int gold) {
    }

    private QuestReward rewardFor(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> new QuestReward("Slime", "SLIME", 100, List.of(45, 55), 50, 10);
            case "MEDIUM" -> new QuestReward("Skeleton", "SKELETON", 200, List.of(95, 105), 100, 25);
            case "HARD" -> new QuestReward("Fierce Orc", "ORC", 300, List.of(147, 153), 250, 60);
            case "EPIC" -> new QuestReward("Dragon", "DRAGON", 500, List.of(160, 175, 165), 500, 120);
            default -> new QuestReward("Slime", "SLIME", 100, List.of(45, 55), 50, 10);
        };
    }

    private String monsterName(String type) {
        return switch (type) {
            case "SLIME" -> "Slime";
            case "SKELETON" -> "Skeleton";
            case "ORC" -> "Fierce Orc";
            case "DRAGON" -> "Dragon";
            default -> type;
        };
    }
}