package com.dungeontasks.inventory.controller;

import static com.dungeontasks.common.config.OpenApiConfiguration.BEARER_SCHEME;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.inventory.dto.response.InventoryResponse;
import com.dungeontasks.inventory.dto.response.ItemResponse;
import com.dungeontasks.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Inventory operations")
@SecurityRequirement(name = BEARER_SCHEME)
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(operationId = "listInventory", summary = "List inventory items")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InventoryResponse> listInventory(@AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(inventoryService.list(currentUser.userID()));
    }

    @GetMapping("/{userItemID}")
    @Operation(operationId = "getInventoryItem", summary = "Get inventory item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ItemResponse> getItem(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID userItemID
    ) {
        return ResponseEntity.ok(inventoryService.get(currentUser.userID(), userItemID));
    }
}