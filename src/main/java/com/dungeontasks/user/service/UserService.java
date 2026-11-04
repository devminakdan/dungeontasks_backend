package com.dungeontasks.user.service;

import java.security.SecureRandom;
import java.util.UUID;

import com.dungeontasks.common.error.ResourceNotFoundException;
import com.dungeontasks.common.error.UsernameTakenException;
import com.dungeontasks.user.domain.UserProfile;
import com.dungeontasks.user.repository.UserRepository;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final String[] ADJECTIVES = {
            "brave", "calm", "clever", "fierce", "lucky", "mighty", "swift", "wise"
    };
    private static final String[] NOUNS = {
            "archer", "dragon", "knight", "mage", "ranger", "rogue", "titan", "wizard"
    };

    private final UserRepository userRepository;
    private final UsernameMutationService usernameMutationService;
    private final SecureRandom random;

    public UserService(UserRepository userRepository, UsernameMutationService usernameMutationService) {
        this.userRepository = userRepository;
        this.usernameMutationService = usernameMutationService;
        this.random = new SecureRandom();
    }

    public UserProfile getProfile(UUID userID) {
        return requireProfile(userID);
    }

    public UserProfile updateUsername(UUID userID, String username) {
        try {
            usernameMutationService.update(userID, username);
        } catch (DataIntegrityViolationException | IntegrityConstraintViolationException exception) {
            throw new UsernameTakenException();
        }
        return requireProfile(userID);
    }

    public String suggestUsername() {
        for (int attempt = 0; attempt < 32; attempt++) {
            String candidate = ADJECTIVES[random.nextInt(ADJECTIVES.length)]
                    + NOUNS[random.nextInt(NOUNS.length)]
                    + (1000 + random.nextInt(9000));
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        String fallback = "hero" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (userRepository.existsByUsername(fallback)) {
            return suggestUsername();
        }
        return fallback;
    }

    private UserProfile requireProfile(UUID userID) {
        return userRepository.findProfileByUserID(userID)
                .orElseThrow(() -> new ResourceNotFoundException("The authenticated user no longer exists"));
    }
}
