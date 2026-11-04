package com.dungeontasks.inventory.domain;

import java.time.Instant;
import java.util.UUID;

public record UserItem(
        UUID userItemID,
        UUID userID,
        ItemDefinition item,
        boolean equipped,
        Instant obtainedAt
) {
}