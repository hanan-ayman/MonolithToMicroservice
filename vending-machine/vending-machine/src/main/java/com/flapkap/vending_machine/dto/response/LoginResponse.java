package com.flapkap.vending_machine.dto.response;

public record LoginResponse(
        String token,
        String username,
        String role
) {}
