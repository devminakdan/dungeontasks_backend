package com.dungeontasks.auth.controller;

import static com.dungeontasks.common.config.OpenApiConfiguration.BEARER_SCHEME;

import com.dungeontasks.auth.dto.request.AuthRequests;
import com.dungeontasks.auth.dto.response.AuthResponses;
import com.dungeontasks.auth.security.CurrentUser;
import com.dungeontasks.auth.service.AuthService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Google sign-in and DungeonTasks session lifecycle")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    @Operation(
            operationId = "loginWithGoogle",
            summary = "Exchange a Google ID token",
            description = "Verifies a Google RS256 ID token, finds or creates the account by Google sub, "
                    + "and creates an independent 30-day DungeonTasks session."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Google token accepted and session created",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponses.AuthResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Google token signature or claims are invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Google Sign-In is not configured on the server",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponses.AuthResponse> loginWithGoogle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Google ID token returned by native Google Sign-In",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthRequests.GoogleLoginRequest.class),
                            examples = @ExampleObject(value = "{\"identityToken\":\"eyJhbGciOiJSUzI1NiJ9...\"}")
                    )
            )
            @Valid @RequestBody AuthRequests.GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(AuthResponses.from(authService.loginWithGoogle(request.identityToken())));
    }

    @PostMapping("/refresh")
    @Operation(
            operationId = "refreshSession",
            summary = "Rotate a refresh token",
            description = "Consumes the current single-use refresh token and returns a new token pair. "
                    + "The original absolute session expiry is preserved. Reuse of a replaced token revokes "
                    + "that device session, including its latest access and refresh tokens."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh token rotated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponses.AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired, reused, or revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponses.AuthResponse> refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Current single-use DungeonTasks refresh token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthRequests.RefreshTokenRequest.class)
                    )
            )
            @Valid @RequestBody AuthRequests.RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(AuthResponses.from(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    @Operation(
            operationId = "logoutCurrentSession",
            summary = "Sign out the current device session",
            description = "Revokes only the session identified by the bearer access token."
    )
    @SecurityRequirement(name = BEARER_SCHEME)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Current session revoked"),
            @ApiResponse(responseCode = "401", description = "Access token is missing, invalid, expired, or revoked",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user cannot perform the operation",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CurrentUser currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.noContent().build();
    }
}
