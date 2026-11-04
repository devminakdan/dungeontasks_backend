package com.dungeontasks.auth.domain;

public record GoogleIdentity(
        String subject,
        String email,
        String avatarURL
) {
}
