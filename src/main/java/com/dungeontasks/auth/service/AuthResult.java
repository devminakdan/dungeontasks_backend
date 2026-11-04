package com.dungeontasks.auth.service;

import com.dungeontasks.auth.security.TokenPair;
import com.dungeontasks.user.domain.UserProfile;

public record AuthResult(
        TokenPair tokenPair,
        UserProfile profile
) {
}
