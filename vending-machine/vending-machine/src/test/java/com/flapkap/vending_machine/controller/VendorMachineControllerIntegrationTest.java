package com.flapkap.vending_machine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flapkap.vending_machine.dto.*;
import com.flapkap.vending_machine.entity.ProductEntity;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.repository.ProductRepository;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
class VendorMachineControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;
    private UserEntity testBuyer;
    private UserEntity testSeller;
    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        User buyerUser = new User("buyer@test.com", "password123", 100, Set.of(Role.BUYER));
        User sellerUser = new User("seller@test.com", "password123", 0, Set.of(Role.SELLER));
        
        authService.signup(buyerUser);
        authService.signup(sellerUser);
        
        testBuyer = userRepository.findByUsername("buyer@test.com").orElseThrow();
        testSeller = userRepository.findByUsername("seller@test.com").orElseThrow();

        // Create test product
        testProduct = new ProductEntity();
        testProduct.setProductName("Test Cola");
        testProduct.setCost(50);
        testProduct.setAmountAvailable(10);
        testProduct.setSellerId(testSeller);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void deposit_ShouldAddToBalance_WhenValidAmount() throws Exception {
        DepositRequest depositRequest = new DepositRequest(50);

        mockMvc.perform(post("/api/v1/vendors/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully deposited 50 cents. New balance: 150 cents"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void deposit_ShouldReturnBadRequest_WhenInvalidAmount() throws Exception {
        DepositRequest invalidRequest = new DepositRequest(25); // Not allowed coin

        mockMvc.perform(post("/api/v1/vendors/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.message").value("Only 5, 10, 20, 50, or 100 cent coins are accepted"));
    }

    @Test
    @WithMockUser(roles = "SELLER")  // Authenticated as SELLER but needs BUYER role
    void deposit_ShouldReturnForbidden_WhenNotBuyer() throws Exception {
        DepositRequest depositRequest = new DepositRequest(50);

        mockMvc.perform(post("/api/v1/vendors/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void buy_ShouldPurchaseProduct_WhenSufficientFunds() throws Exception {
        // First deposit money
        testBuyer.setDeposit(100);
        userRepository.save(testBuyer);

        PurchaseItem item = new PurchaseItem(testProduct.getProductName(), 1);
        BuyRequest buyRequest = new BuyRequest(List.of(item));

        mockMvc.perform(post("/api/v1/vendors/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Purchase successful!")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Cola x 1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Total spent: 50 cents")));
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void buy_ShouldReturnBadRequest_WhenInsufficientFunds() throws Exception {
        // Set low balance
        testBuyer.setDeposit(25);
        userRepository.save(testBuyer);

        PurchaseItem item = new PurchaseItem(testProduct.getProductName(), 1);
        BuyRequest buyRequest = new BuyRequest(List.of(item));

        mockMvc.perform(post("/api/v1/vendors/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void buy_ShouldReturnNotFound_WhenProductNotExists() throws Exception {
        testBuyer.setDeposit(100);
        userRepository.save(testBuyer);

        PurchaseItem item = new PurchaseItem("Non-existent Product", 1);
        BuyRequest buyRequest = new BuyRequest(List.of(item));

        mockMvc.perform(post("/api/v1/vendors/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void buy_ShouldReturnBadRequest_WhenInsufficientStock() throws Exception {
        testBuyer.setDeposit(1000);
        userRepository.save(testBuyer);

        // Try to buy more than available
        PurchaseItem item = new PurchaseItem(testProduct.getProductName(), 15); // Only 10 available
        BuyRequest buyRequest = new BuyRequest(List.of(item));

        mockMvc.perform(post("/api/v1/vendors/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    @WithMockUser(roles = "SELLER")  // Authenticated as SELLER but needs BUYER role
    void buy_ShouldReturnForbidden_WhenNotBuyer() throws Exception {
        PurchaseItem item = new PurchaseItem(testProduct.getProductName(), 1);
        BuyRequest buyRequest = new BuyRequest(List.of(item));

        mockMvc.perform(post("/api/v1/vendors/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void reset_ShouldResetDeposit_WhenCalled() throws Exception {
        // Set some deposit first
        testBuyer.setDeposit(75);
        userRepository.save(testBuyer);

        mockMvc.perform(post("/api/v1/vendors/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit reset successfully. Returned: 1 x 50 cents, 1 x 20 cents, 1 x 5 cents"));
    }

    @Test
    @WithMockUser(roles = "SELLER")  // Authenticated as SELLER but needs BUYER role
    void reset_ShouldReturnForbidden_WhenNotBuyer() throws Exception {
        mockMvc.perform(post("/api/v1/vendors/reset"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "buyer@test.com", roles = "BUYER")
    void buy_ShouldHandleMultipleItems_WhenValidRequest() throws Exception {
        // Create another product
        ProductEntity product2 = new ProductEntity();
        product2.setProductName("Test Chips");
        product2.setCost(30);
        product2.setAmountAvailable(5);
        product2.setSellerId(testSeller);
        productRepository.save(product2);

        testBuyer.setDeposit(200);
        userRepository.save(testBuyer);

        PurchaseItem item1 = new PurchaseItem(testProduct.getProductName(), 2);
        PurchaseItem item2 = new PurchaseItem(product2.getProductName(), 1);
        BuyRequest buyRequest = new BuyRequest(List.of(item1, item2));

        mockMvc.perform(post("/api/v1/vendors/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Purchase successful!")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Cola x 2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Chips x 1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Total spent: 130 cents")));
    }
}
