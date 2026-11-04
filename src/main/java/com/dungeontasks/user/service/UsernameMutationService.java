package com.dungeontasks.user.service;

import java.time.Clock;
import java.util.UUID;

import com.dungeontasks.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsernameMutationService {

    private final UserRepository userRepository;
    private final Clock clock;

    public UsernameMutationService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void update(UUID userID, String username) {
        userRepository.updateUsername(userID, username, clock.instant());
    }
}
