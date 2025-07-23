package com.flapkap.vending_machine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flapkap.vending_machine.dto.Role;
import com.flapkap.vending_machine.dto.User;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "BUYER")
    void updateUser_ShouldUpdateUser_WhenValidData() throws Exception {
        // Create user first
        User originalUser = new User("testuser@example.com", "password123", 50, Set.of(Role.BUYER));
        authService.signup(originalUser);

        User updateRequest = new User("testuser@example.com", "newpassword123", 100, Set.of(Role.SELLER));

        mockMvc.perform(put("/api/v1/users/testuser@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "BUYER")
    void updateUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        User updateRequest = new User("nonexistent@example.com", "password123", 100, Set.of(Role.BUYER));

        mockMvc.perform(put("/api/v1/users/nonexistent@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "BUYER")
    void updateUser_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        // Create user first
        User originalUser = new User("testuser@example.com", "password123", 50, Set.of(Role.BUYER));
        authService.signup(originalUser);

        User invalidUpdate = new User("", "", -100, null);

        mockMvc.perform(put("/api/v1/users/testuser@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        User updateRequest = new User("testuser@example.com", "password123", 100, Set.of(Role.BUYER));

        mockMvc.perform(put("/api/v1/users/testuser@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "BUYER")
    void deleteUser_ShouldDeleteUser_WhenUserExists() throws Exception {
        // Create user first
        User user = new User("testuser@example.com", "password123", 50, Set.of(Role.BUYER));
        authService.signup(user);

        mockMvc.perform(delete("/api/v1/users/testuser@example.com"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "BUYER")
    void deleteUser_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/v1/users/nonexistent@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/testuser@example.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "SELLER")
    void updateUser_ShouldAllowRoleChange_WhenValidRequest() throws Exception {
        // Create user first
        User originalUser = new User("testuser@example.com", "password123", 0, Set.of(Role.BUYER));
        authService.signup(originalUser);

        User roleChangeRequest = new User("testuser@example.com", null, 0, Set.of(Role.SELLER));

        mockMvc.perform(put("/api/v1/users/testuser@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleChangeRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "BUYER")
    void updateUser_ShouldReturnBadRequest_WhenUsernameAlreadyExists() throws Exception {
        // Create two users
        User user1 = new User("user1@example.com", "password123", 0, Set.of(Role.BUYER));
        User user2 = new User("user2@example.com", "password123", 0, Set.of(Role.BUYER));
        authService.signup(user1);
        authService.signup(user2);

        // Try to change user2's username to user1's username
        User updateRequest = new User("user1@example.com", null, -1, null);

        mockMvc.perform(put("/api/v1/users/user2@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }
}
