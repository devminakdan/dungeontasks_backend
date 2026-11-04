package com.dungeontasks.quest.dto.response;

import java.util.List;

public record QuestListResponse(
        List<QuestResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}