package com.dungeontasks.user.dto.response;

import java.util.UUID;

import com.dungeontasks.user.domain.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

public final class UserResponses {

    private UserResponses() {
    }

    @Schema(name = "UserProfile", description = "Current DungeonTasks user profile")
    public record UserProfileResponse(
            @Schema(
                    description = "Stable DungeonTasks user identifier",
                    example = "1ec7939d-c0c5-4c8a-b53f-24f0881014fb",
                    format = "uuid",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            UUID userID,
            @Schema(
                    description = "Chosen unique username; null until onboarding is completed",
                    example = "swiftknight42",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String username,
            @Schema(
                    description = "Latest verified email supplied by Google",
                    example = "hero@example.com",
                    format = "email",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String email,
            @Schema(
                    description = "Latest Google profile image URL, when supplied",
                    example = "https://lh3.googleusercontent.com/a/example",
                    format = "uri",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String avatarURL
    ) {
    }

    @Schema(name = "UsernameSuggestion", description = "Available username suggestion that has not been saved")
    public record UsernameSuggestionResponse(
            @Schema(
                    description = "Currently available editable username suggestion",
                    example = "bravedragon4821",
                    pattern = "^[a-z0-9]{3,24}$",
                    minLength = 3,
                    maxLength = 24,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String username
    ) {
    }

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.userID(),
                profile.username(),
                profile.email(),
                profile.avatarURL()
        );
    }
}
