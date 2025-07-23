package com.flapkap.vending_machine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BuyRequest(
        @NotEmpty(message = "Items list cannot be empty")
        @Valid
        List<PurchaseItem> items
) {
}
