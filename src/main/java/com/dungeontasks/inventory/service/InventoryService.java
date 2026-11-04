package com.dungeontasks.inventory.service;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.inventory.domain.UserItem;
import com.dungeontasks.inventory.dto.response.InventoryResponse;
import com.dungeontasks.inventory.dto.response.ItemResponse;
import com.dungeontasks.inventory.repository.InventoryRepository;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public InventoryResponse list(UUID userID) {
        List<ItemResponse> items = inventoryRepository.findByUser(userID).stream()
                .map(this::toResponse)
                .toList();
        return new InventoryResponse(items, items.size());
    }

    public ItemResponse get(UUID userID, UUID userItemID) {
        UserItem ui = inventoryRepository.findUserItem(userItemID)
                .orElseThrow(() -> new com.dungeontasks.common.error.ResourceNotFoundException("Item not found"));
        if (!ui.userID().equals(userID)) {
            throw new com.dungeontasks.common.error.ForbiddenException("Item belongs to another user");
        }
        return toResponse(ui);
    }

    public ItemResponse toResponse(UserItem ui) {
        return new ItemResponse(
                ui.userItemID(),
                ui.item().itemID(),
                ui.item().name(),
                ui.item().description(),
                ui.item().rarity(),
                ui.item().type(),
                ui.item().attackBonus(),
                ui.item().defenseBonus(),
                ui.item().luckBonus(),
                ui.equipped(),
                ui.obtainedAt()
        );
    }
}