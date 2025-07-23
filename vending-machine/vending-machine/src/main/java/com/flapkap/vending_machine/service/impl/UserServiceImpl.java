package com.flapkap.vending_machine.service.impl;

import com.flapkap.vending_machine.dto.User;
import com.flapkap.vending_machine.exception.ResourceNotFoundException;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;


    @Override
    public void updateUser(String userName, User user) {
        log.info("Updating user with username: {}", userName);

        UserEntity existingUser = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + userName));

        // Check for username uniqueness if it's being updated
        if (user.username() != null && !user.username().equals(existingUser.getUsername())) {
            userRepository.findByUsername(user.username())
                    .ifPresent(u -> {
                        throw new IllegalArgumentException("Username already exists: " + user.username());
                    });
            existingUser.setUsername(user.username());
        }

        if (user.deposit() < 0) {
            existingUser.setDeposit(user.deposit());
        }

        if (user.roles() != null && !user.roles().isEmpty()) {
            existingUser.setRoles(user.roles());
        }

        if (user.password() != null) {
            existingUser.setPassword(passwordEncoder.encode(user.password()));
        }

        userRepository.save(existingUser);
        log.info("User with username: {} updated successfully", userName);
    }

    @Override
    public void deleteUser(String userName) {
        log.info("Deleting user with username: {}", userName);

        UserEntity user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + userName));

        userRepository.delete(user);
        log.info("User with username: {} deleted successfully", userName);
    }
}
