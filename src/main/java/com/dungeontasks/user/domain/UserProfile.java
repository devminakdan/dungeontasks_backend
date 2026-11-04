package com.dungeontasks.user.domain;

import java.util.UUID;

public record UserProfile(
        UUID userID,
        String username,
        String email,
        String avatarURL
) {
}
