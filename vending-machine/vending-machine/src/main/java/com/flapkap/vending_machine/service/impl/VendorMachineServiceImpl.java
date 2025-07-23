package com.flapkap.vending_machine.service.impl;

import com.flapkap.vending_machine.dto.*;
import com.flapkap.vending_machine.entity.ProductEntity;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.exception.ResourceNotFoundException;
import com.flapkap.vending_machine.repository.ProductRepository;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.service.VendorMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VendorMachineServiceImpl implements VendorMachineService {

    private static final List<Integer> ALLOWED_COINS = List.of(5, 10, 20, 50, 100);
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public String deposit(@Valid DepositRequest request) {
        int amount = request.amount();
        log.info("Processing deposit request for amount: {} cents", amount);
        
        if (!ALLOWED_COINS.contains(amount)) {
            log.warn("Invalid coin amount attempted: {} cents", amount);
            throw new IllegalArgumentException("Only 5, 10, 20, 50, or 100 cent coins are accepted");
        }

        try {
            UserEntity currentUser = getCurrentUser();
            int oldBalance = currentUser.getDeposit();
            currentUser.setDeposit(oldBalance + amount);
            userRepository.save(currentUser);
            
            log.info("Successfully deposited {} cents for user: {}. Balance: {} -> {}", 
                    amount, currentUser.getUsername(), oldBalance, currentUser.getDeposit());
            
            return String.format("Successfully deposited %d cents. New balance: %d cents", 
                amount, currentUser.getDeposit());
        } catch (Exception e) {
            log.error("Error processing deposit for amount: {} cents", amount, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public String buy(@Valid BuyRequest request) {
        log.info("Processing buy request with {} items", request.items().size());
        
        try {
            UserEntity buyer = getCurrentUser();
            log.info("Buy request from user: {} with balance: {} cents", buyer.getUsername(), buyer.getDeposit());
            
            List<String> purchaseDetails = new ArrayList<>();
            int totalCost = 0;
            
            // Validate all items first and calculate total cost
            Map<ProductEntity, Integer> itemsToPurchase = new HashMap<>();
            
            for (PurchaseItem item : request.items()) {
                log.debug("Validating item: {} quantity: {}", item.productName(), item.amountOfProducts());
                
                ProductEntity product = productRepository.findByProductName(item.productName())
                    .orElseThrow(() -> {
                        log.warn("Product not found: {}", item.productName());
                        return new ResourceNotFoundException("Product not found with name: " + item.productName());
                    });
                
                // Validate stock
                if (product.getAmountAvailable() < item.amountOfProducts()) {
                    log.warn("Insufficient stock for product: {}. Available: {}, Requested: {}", 
                            item.productName(), product.getAmountAvailable(), item.amountOfProducts());
                    throw new IllegalArgumentException("Insufficient stock for product: " + item.productName() + 
                        ". Available: " + product.getAmountAvailable() + ", Requested: " + item.amountOfProducts());
                }
                
                int itemCost = product.getCost() * item.amountOfProducts();
                totalCost += itemCost;
                itemsToPurchase.put(product, item.amountOfProducts());
                
                purchaseDetails.add(String.format("%s x %d (cost: %d cents)", 
                    item.productName(), item.amountOfProducts(), itemCost));
            }
            
            log.info("Total cost calculated: {} cents for user: {}", totalCost, buyer.getUsername());
            
            // Check if buyer has enough money
            if (buyer.getDeposit() < totalCost) {
                log.warn("Insufficient funds for user: {}. Balance: {}, Required: {}", 
                        buyer.getUsername(), buyer.getDeposit(), totalCost);
                throw new IllegalArgumentException("Insufficient funds. Balance: " + buyer.getDeposit() + 
                    " cents, Required: " + totalCost + " cents");
            }
            
            // Process the purchase
            for (Map.Entry<ProductEntity, Integer> entry : itemsToPurchase.entrySet()) {
                ProductEntity product = entry.getKey();
                int quantity = entry.getValue();
                
                product.setAmountAvailable(product.getAmountAvailable() - quantity);
                productRepository.save(product);
                
                log.debug("Updated stock for product: {}. New amount: {}", 
                        product.getProductName(), product.getAmountAvailable());
            }
            
            // Calculate change
            Map<Integer, Integer> change = calculateChange(buyer.getDeposit() - totalCost);
            buyer.setDeposit(0); // Reset deposit after purchase
            userRepository.save(buyer);
            
            log.info("Purchase completed successfully for user: {}. Total spent: {} cents", 
                    buyer.getUsername(), totalCost);
            
            // Build response message
            StringBuilder response = new StringBuilder();
            response.append("Purchase successful!\n");
            response.append("Items purchased:\n");
            for (String detail : purchaseDetails) {
                response.append("- ").append(detail).append("\n");
            }
            response.append("Total spent: ").append(totalCost).append(" cents\n");
            response.append("Change: ").append(formatChange(change));
            
            return response.toString();
        } catch (Exception e) {
            log.error("Error processing buy request", e);
            throw e;
        }
    }

    @Override
    public String reset() {
        log.info("Processing reset request");
        
        try {
            UserEntity currentUser = getCurrentUser();
            int currentDeposit = currentUser.getDeposit();
            
            log.info("Resetting deposit for user: {}. Current deposit: {} cents", 
                    currentUser.getUsername(), currentDeposit);
            
            if (currentDeposit == 0) {
                log.info("No deposit to reset for user: {}", currentUser.getUsername());
                return "No deposit to reset";
            }
            
            Map<Integer, Integer> change = calculateChange(currentDeposit);
            currentUser.setDeposit(0);
            userRepository.save(currentUser);
            
            log.info("Successfully reset deposit for user: {}. Returned: {} cents", 
                    currentUser.getUsername(), currentDeposit);
            
            return "Deposit reset successfully. Returned: " + formatChange(change);
        } catch (Exception e) {
            log.error("Error processing reset request", e);
            throw e;
        }
    }

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.debug("Getting current user: {}", username);
        
        return userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("User not found: {}", username);
                return new ResourceNotFoundException("User not found with username: " + username);
            });
    }

    private Map<Integer, Integer> calculateChange(int amount) {
        log.debug("Calculating change for amount: {} cents", amount);
        
        Map<Integer, Integer> change = new LinkedHashMap<>();
        int[] coins = {100, 50, 20, 10, 5};
        
        for (int coin : coins) {
            if (amount >= coin) {
                int count = amount / coin;
                change.put(coin, count);
                amount %= coin;
                log.debug("Change: {} x {} cent coins", count, coin);
            }
        }
        
        return change;
    }
    
    private String formatChange(Map<Integer, Integer> change) {
        if (change.isEmpty()) {
            return "No change";
        }
        
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : change.entrySet()) {
            parts.add(String.format("%d x %d cent%s", 
                entry.getValue(), 
                entry.getKey(),
                entry.getKey() != 1 ? "s" : ""));
        }
        return String.join(", ", parts);
    }
}
