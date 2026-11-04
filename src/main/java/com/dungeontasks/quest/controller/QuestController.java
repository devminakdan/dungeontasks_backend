package com.dungeontasks.quest.controller;

import static com.dungeontasks.common.config.OpenApiConfiguration.BEARER_SCHEME;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.quest.domain.QuestCategory;
import com.dungeontasks.quest.domain.QuestDifficulty;
import com.dungeontasks.quest.domain.QuestStatus;
import com.dungeontasks.quest.dto.request.CreateQuestRequest;
import com.dungeontasks.quest.dto.request.UpdateQuestRequest;
import com.dungeontasks.quest.dto.response.QuestListResponse;
import com.dungeontasks.quest.dto.response.QuestResponse;
import com.dungeontasks.quest.dto.response.BattleResultResponse;
import com.dungeontasks.quest.service.BattleEngineService;
import com.dungeontasks.quest.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quests")
@Tag(name = "Quests", description = "Quest management operations")
@SecurityRequirement(name = BEARER_SCHEME)
public class QuestController {

    private final QuestService questService;
    private final com.dungeontasks.quest.service.BattleEngineService battleEngineService;

    public QuestController(QuestService questService, com.dungeontasks.quest.service.BattleEngineService battleEngineService) {
        this.questService = questService;
        this.battleEngineService = battleEngineService;
    }

    @GetMapping
    @Operation(operationId = "listQuests", summary = "List quests for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuestListResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<QuestListResponse> listQuests(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) QuestStatus status,
            @RequestParam(required = false) QuestCategory category,
            @RequestParam(required = false) QuestDifficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(questService.list(
                currentUser.userID(),
                status == null ? null : status.name(),
                category == null ? null : category.name(),
                difficulty == null ? null : difficulty.name(),
                page,
                size
        ));
    }

    @PostMapping
    @Operation(operationId = "createQuest", summary = "Create a quest")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created quest",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<QuestResponse> createQuest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateQuestRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questService.create(currentUser.userID(), request));
    }

    @GetMapping("/{questID}")
    @Operation(operationId = "getQuest", summary = "Get quest details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuestResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<QuestResponse> getQuest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID questID
    ) {
        return ResponseEntity.ok(questService.get(currentUser.userID(), questID));
    }

    @PatchMapping("/{questID}")
    @Operation(operationId = "updateQuest", summary = "Update a quest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated quest",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = QuestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<QuestResponse> updateQuest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID questID,
            @Valid @RequestBody UpdateQuestRequest request
    ) {
        return ResponseEntity.ok(questService.update(currentUser.userID(), questID, request));
    }

    @PostMapping("/{questID}/complete")
    @Operation(operationId = "completeQuest", summary = "Complete a quest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Battle result",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BattleResultResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<BattleResultResponse> completeQuest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID questID
    ) {
        return ResponseEntity.ok(battleEngineService.complete(currentUser.userID(), questID));
    }

    @DeleteMapping("/{questID}")
    @Operation(operationId = "deleteQuest", summary = "Delete a quest")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteQuest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID questID
    ) {
        questService.delete(currentUser.userID(), questID);
        return ResponseEntity.noContent().build();
    }
}