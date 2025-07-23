package com.flapkap.vending_machine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flapkap.vending_machine.dto.Role;
import com.flapkap.vending_machine.dto.User;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.service.AuthService;
import com.flapkap.vending_machine.util.MappingUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = {VendingMachineApplication.class, TestConfig.class})
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class VendingMachineApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MappingUtil mappingUtil;

    private static final String TEST_USERNAME = "testuser@example.com";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Application context should have loaded");
    }

    @Test
    void testUserSignup() {
        // Given
        User newUser = new User(
            TEST_USERNAME,
            TEST_PASSWORD,
            100,
            Set.of(Role.BUYER)
        );

        // When
        User createdUser = authService.signup(newUser);

        // Then
        assertNotNull(createdUser, "Created user should not be null");
        assertEquals(TEST_USERNAME, createdUser.username(), "Usernames should match");
        assertEquals(100, createdUser.deposit(), "Deposit should match");
        assertTrue(createdUser.roles().contains(Role.BUYER), "User should have BUYER role");
        
        // Verify password is not exposed in the response
        assertTrue(createdUser.password() == null || createdUser.password().isEmpty(), 
            "Password should not be exposed in the response");
            
        // Verify user was saved correctly in the database
        UserEntity savedUser = userRepository.findByUsername(TEST_USERNAME)
            .orElseThrow(() -> new AssertionError("User should exist in the database"));
            
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPassword()),
            "Password should be properly encoded in the database");
    }
}
