package com.flapkap.vending_machine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Product(
        @Min(value = 0, message = "Amount available cannot be negative")
        int amountAvailable,
        
        @Positive(message = "Cost must be a positive number")
        int cost,
        
        @NotBlank(message = "Product name is required")
        String productName,
        
        @NotNull(message = "Seller ID is required")
        Long sellerId
) { }



