package com.flapkap.vending_machine.service;

import com.flapkap.vending_machine.dto.BuyRequest;
import com.flapkap.vending_machine.dto.DepositRequest;
import jakarta.validation.Valid;

public interface VendorMachineService {
    String deposit(@Valid DepositRequest request);
    String buy(@Valid BuyRequest request);
    String reset();
}
