package com.dungeontasks.character.controller;

import static com.dungeontasks.common.config.OpenApiConfiguration.BEARER_SCHEME;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.character.dto.response.CharacterResponse;
import com.dungeontasks.character.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/character")
@Tag(name = "Character", description = "Character profile operations")
@SecurityRequirement(name = BEARER_SCHEME)
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping
    @Operation(operationId = "getCharacter", summary = "Get the current character")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Character",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CharacterResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CharacterResponse> getCharacter(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(characterService.toResponse(
                characterService.getOrCreate(currentUser.userID())
        ));
    }
}