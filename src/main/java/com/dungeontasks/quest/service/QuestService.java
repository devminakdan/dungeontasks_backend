package com.dungeontasks.quest.service;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.common.error.ForbiddenException;
import com.dungeontasks.common.error.ApiException;
import com.dungeontasks.common.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.dungeontasks.quest.domain.Quest;
import com.dungeontasks.quest.domain.QuestStatus;
import com.dungeontasks.quest.dto.request.CreateQuestRequest;
import com.dungeontasks.quest.dto.request.UpdateQuestRequest;
import com.dungeontasks.quest.dto.response.QuestResponse;
import com.dungeontasks.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestService {

    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    @Transactional
    public QuestResponse create(UUID userID, CreateQuestRequest request) {
        Instant now = Instant.now();
        Quest quest = questRepository.insertQuest(userID, request, now);
        return toResponse(quest);
    }

    public QuestResponse get(UUID currentUserID, UUID questID) {
        Quest quest = requireOwnedQuest(currentUserID, questID);
        return toResponse(quest);
    }

    public com.dungeontasks.quest.dto.response.QuestListResponse list(
            UUID currentUserID,
            String status,
            String category,
            String difficulty,
            int page,
            int size
    ) {
        if (status != null && !status.isBlank()) {
            QuestStatus.valueOf(status);
        }
        if (category != null && !category.isBlank()) {
            com.dungeontasks.quest.domain.QuestCategory.valueOf(category);
        }
        if (difficulty != null && !difficulty.isBlank()) {
            com.dungeontasks.quest.domain.QuestDifficulty.valueOf(difficulty);
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        String normalizedStatus = (status == null || status.isBlank()) ? null : status;
        String normalizedCategory = (category == null || category.isBlank()) ? null : category;
        String normalizedDifficulty = (difficulty == null || difficulty.isBlank()) ? null : difficulty;

        List<Quest> quests = questRepository.findByUser(
                currentUserID, normalizedStatus, normalizedCategory, normalizedDifficulty, safePage, safeSize);
        long total = questRepository.countByUser(
                currentUserID, normalizedStatus, normalizedCategory, normalizedDifficulty);

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return new com.dungeontasks.quest.dto.response.QuestListResponse(
                quests.stream().map(this::toResponse).toList(),
                safePage,
                safeSize,
                total,
                totalPages
        );
    }

    @Transactional
    public QuestResponse update(UUID currentUserID, UUID questID, UpdateQuestRequest request) {
        Quest quest = requireOwnedQuest(currentUserID, questID);

        if (request.status() != null) {
            if (request.status() != QuestStatus.ACTIVE && request.status() != QuestStatus.CANCELLED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Bad request", "Only ACTIVE or CANCELLED status can be set");
            }
        }
        questRepository.updateQuest(questID, request, Instant.now());

        return toResponse(questRepository.findById(questID).orElseThrow(
                () -> new ResourceNotFoundException("Quest with ID '" + questID + "' does not exist")
        ));
    }

    @Transactional
    public void delete(UUID currentUserID, UUID questID) {
        Quest quest = requireOwnedQuest(currentUserID, questID);
        questRepository.deleteQuest(quest.questID());
    }

    private Quest requireOwnedQuest(UUID currentUserID, UUID questID) {
        Quest quest = questRepository.findById(questID)
                .orElseThrow(() -> new ResourceNotFoundException("Quest with ID '" + questID + "' does not exist"));

        if (!quest.userID().equals(currentUserID)) {
            throw new ForbiddenException("Quest with ID '" + questID + "' belongs to another user");
        }
        return quest;
    }

    private QuestResponse toResponse(Quest quest) {
        return new QuestResponse(
                quest.questID(),
                quest.userID(),
                quest.title(),
                quest.description(),
                quest.difficulty(),
                quest.category(),
                quest.deadline(),
                quest.status(),
                quest.createdAt(),
                quest.updatedAt(),
                quest.completedAt()
        );
    }
}