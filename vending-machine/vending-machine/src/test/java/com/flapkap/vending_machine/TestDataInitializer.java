package com.flapkap.vending_machine;

import com.flapkap.vending_machine.dto.Role;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@Profile("test")
public class TestDataInitializer {

    @Bean
    public CommandLineRunner initTestData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create a test user
            UserEntity testUser = new UserEntity();
            testUser.setUsername("testuser");
            testUser.setPassword(passwordEncoder.encode("password"));
            testUser.setDeposit(0);
            testUser.setRoles(Set.of(Role.BUYER));
            
            userRepository.save(testUser);
        };
    }
}
