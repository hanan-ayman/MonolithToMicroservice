package com.flapkap.vending_machine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import java.util.Set;

public record User(
        @NotBlank(message = "Username is required")
        String username,
        
        @Nullable 
        String password,
        
        @Min(value = 0, message = "Deposit cannot be negative")
        int deposit,
        
        @NotNull(message = "Roles are required")
        Set<Role> roles
) { }
