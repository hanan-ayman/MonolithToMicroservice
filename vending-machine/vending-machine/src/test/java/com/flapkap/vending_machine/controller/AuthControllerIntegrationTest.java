package com.flapkap.vending_machine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flapkap.vending_machine.dto.LoginRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
class AuthControllerIntegrationTest {

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
    void signup_ShouldCreateUser_WhenValidData() throws Exception {
        User newUser = new User("test@example.com", "password123", 100, Set.of(Role.BUYER));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.deposit").value(100))
                .andExpect(jsonPath("$.roles[0]").value("BUYER"))
                .andExpect(jsonPath("$.password").value(""));
    }

    @Test
    void signup_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        User invalidUser = new User("", "", -1, null);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_ShouldReturnBadRequest_WhenUserAlreadyExists() throws Exception {
        User user = new User("duplicate@example.com", "password123", 0, Set.of(Role.BUYER));
        
        // Create user first
        authService.signup(user);

        // Try to create same user again
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.message").value("Username already exists: duplicate@example.com"));
    }

//    @Test
//    void login_ShouldReturnToken_WhenValidCredentials() throws Exception {
//        // Create user first
//        User user = new User("login@example.com", "password123", 0, Set.of(Role.SELLER));
//        authService.signup(user);
//
//        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");
//
//        mockMvc.perform(post("/api/v1/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.token").exists())
//                .andExpect(jsonPath("$.username").value("login@example.com"))
//                .andExpect(jsonPath("$.role").value("ROLE_SELLER"));
//    }

    @Test
    void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenMissingData() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_ShouldCreateSellerUser_WhenSellerRole() throws Exception {
        User sellerUser = new User("seller@example.com", "password123", 0, Set.of(Role.SELLER));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("seller@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("SELLER"));
    }

    @Test
    void signup_ShouldCreateBuyerUser_WhenBuyerRole() throws Exception {
        User buyerUser = new User("buyer@example.com", "password123", 50, Set.of(Role.BUYER));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("buyer@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("BUYER"))
                .andExpect(jsonPath("$.deposit").value(50));
    }
}
