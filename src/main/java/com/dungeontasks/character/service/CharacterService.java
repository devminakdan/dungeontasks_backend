package com.dungeontasks.character.service;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.character.domain.Character;
import com.dungeontasks.character.dto.response.CharacterResponse;
import com.dungeontasks.character.repository.CharacterRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;

    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    @Transactional
    public Character getOrCreate(UUID userID) {
        return characterRepository.findByUserID(userID)
                .orElseGet(() -> characterRepository.createCharacter(userID, java.time.Instant.now()));
    }

    public CharacterResponse toResponse(Character character) {
        return new CharacterResponse(
                character.characterID(),
                character.userID(),
                character.level(),
                character.xp(),
                character.xpToNextLevel(),
                character.gold(),
                character.attack(),
                character.defense(),
                character.luck(),
                character.createdAt(),
                character.updatedAt()
        );
    }
}