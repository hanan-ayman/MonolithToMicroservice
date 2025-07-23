package com.flapkap.vending_machine.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flapkap.vending_machine.dto.LoginRequest;
import com.flapkap.vending_machine.dto.response.LoginResponse;
import com.flapkap.vending_machine.dto.User;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.security.JwtService;
import com.flapkap.vending_machine.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        return new LoginResponse(token, request.username(), role);
    }

    @Override
    public User signup(User request) {
        log.info("Processing signup for user: {}", request.username());
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.error("Username already exists: {}", request.username());
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }
        UserEntity userEntity = objectMapper.convertValue(request, UserEntity.class);
        userEntity.setPassword(passwordEncoder.encode(request.password()));
        UserEntity savedEntity = userRepository.save(userEntity);
        User response = objectMapper.convertValue(savedEntity, User.class);
        return new User(
                response.username(),
                "", // Don't expose password
                response.deposit(),
                response.roles()
        );
    }
}
