package com.dungeontasks.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class UserRequests {

    private UserRequests() {
    }

    @Schema(name = "UpdateUsernameRequest", description = "Requested DungeonTasks username")
    public record UpdateUsernameRequest(
            @NotNull
            @Pattern(regexp = "^[a-z0-9]{3,24}$")
            @Schema(
                    description = "Unique lowercase username containing only ASCII letters and digits",
                    example = "swiftknight42",
                    pattern = "^[a-z0-9]{3,24}$",
                    minLength = 3,
                    maxLength = 24
            )
            String username
    ) {
    }
}
