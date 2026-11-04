package com.dungeontasks.user.controller;

import static com.dungeontasks.common.config.OpenApiConfiguration.BEARER_SCHEME;

import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.user.dto.request.UserRequests;
import com.dungeontasks.user.dto.response.UserResponses;
import com.dungeontasks.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Current user", description = "Profile and username operations for the authenticated user")
@SecurityRequirement(name = BEARER_SCHEME)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(
            operationId = "getCurrentUser",
            summary = "Get the current profile",
            description = "Returns the profile belonging to the verified bearer token. No user identifier is accepted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponses.UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Access token is missing, invalid, expired, or revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user cannot access the profile",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Authenticated user no longer exists",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponses.UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(UserResponses.from(userService.getProfile(currentUser.userID())));
    }

    @PutMapping("/username")
    @Operation(
            operationId = "updateCurrentUsername",
            summary = "Set or change the current username",
            description = "Atomically stores a unique lowercase username for the current user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponses.UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Username does not match ^[a-z0-9]{3,24}$",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Access token is missing, invalid, expired, or revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user cannot change this profile",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Authenticated user no longer exists",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Username is already used by another account",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponses.UserProfileResponse> updateUsername(
            @AuthenticationPrincipal CurrentUser currentUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "New username",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRequests.UpdateUsernameRequest.class),
                            examples = @ExampleObject(value = "{\"username\":\"swiftknight42\"}")
                    )
            )
            @Valid @RequestBody UserRequests.UpdateUsernameRequest request
    ) {
        return ResponseEntity.ok(UserResponses.from(
                userService.updateUsername(currentUser.userID(), request.username())
        ));
    }

    @PostMapping("/username-suggestions")
    @Operation(
            operationId = "suggestCurrentUsername",
            summary = "Generate an available username suggestion",
            description = "Returns a currently available editable suggestion without saving it. "
                    + "Availability is rechecked by the database when the username is saved."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unsaved username suggestion",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserResponses.UsernameSuggestionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Access token is missing, invalid, expired, or revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user cannot access this operation",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Authenticated user no longer exists",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserResponses.UsernameSuggestionResponse> suggestUsername(
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        userService.getProfile(currentUser.userID());
        return ResponseEntity.ok(new UserResponses.UsernameSuggestionResponse(userService.suggestUsername()));
    }
}
