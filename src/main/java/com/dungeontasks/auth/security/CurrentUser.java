package com.dungeontasks.auth.security;

import java.util.UUID;

public record CurrentUser(
        UUID userID,
        UUID sessionID
) {
}
