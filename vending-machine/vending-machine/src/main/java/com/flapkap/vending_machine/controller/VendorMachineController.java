package com.flapkap.vending_machine.controller;

import com.flapkap.vending_machine.dto.BuyRequest;
import com.flapkap.vending_machine.dto.DepositRequest;
import com.flapkap.vending_machine.service.VendorMachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendors")
@Validated
@RequiredArgsConstructor
public class VendorMachineController {

    private final VendorMachineService vendorMachineService;

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<String> deposit(@Valid @RequestBody DepositRequest request) {
        String response = vendorMachineService.deposit(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/buy")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<String> buy(@Valid @RequestBody BuyRequest request) {
        String response = vendorMachineService.buy(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<String> reset() {
        String response = vendorMachineService.reset();
        return ResponseEntity.ok(response);
    }
}
