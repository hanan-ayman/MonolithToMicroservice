package com.flapkap.vending_machine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PurchaseItem(
        @NotBlank(message = "Product name is required")
        String productName,
        
        @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be at least 1")
        Integer amountOfProducts
) {
}
