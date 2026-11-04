package com.dungeontasks.inventory.dto.response;

import java.util.List;

public record InventoryResponse(
        List<ItemResponse> items,
        int totalCount
) {
}