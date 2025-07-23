package com.flapkap.vending_machine.dto.response;

import java.util.List;

public record BuyResponse (int totalSpent,
                           List<String> products,
                           int change) {
}
