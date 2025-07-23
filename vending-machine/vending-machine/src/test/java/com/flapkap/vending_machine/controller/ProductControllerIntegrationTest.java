package com.flapkap.vending_machine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flapkap.vending_machine.dto.Product;
import com.flapkap.vending_machine.dto.Role;
import com.flapkap.vending_machine.dto.User;
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

import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWebMvc
@Transactional
class ProductControllerIntegrationTest {

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
    private UserEntity testSeller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test seller
        User sellerUser = new User("seller@test.com", "password123", 0, Set.of(Role.SELLER));
        authService.signup(sellerUser);
        testSeller = userRepository.findByUsername("seller@test.com").orElseThrow();
    }

    @Test
    void getAllProducts_ShouldReturnEmptyList_WhenNoProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void createProduct_ShouldCreateProduct_WhenValidData() throws Exception {
        Product product = new Product(10, 150, "Test Product", testSeller.getId());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productName").value("Test Product"))
                .andExpect(jsonPath("$.cost").value(150))
                .andExpect(jsonPath("$.amountAvailable").value(10))
                .andExpect(jsonPath("$.sellerId").value(testSeller.getId()));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void createProduct_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        Product invalidProduct = new Product(-1, -50, "", null);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "BUYER")  // Authenticated as BUYER but needs SELLER role
    void createProduct_ShouldReturnForbidden_WhenNotSeller() throws Exception {
        Product product = new Product(10, 150, "Test Product", testSeller.getId());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void updateProduct_ShouldUpdateProduct_WhenValidData() throws Exception {
        // First create a product
        Product originalProduct = new Product(10, 150, "Original Product", testSeller.getId());
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalProduct)));

        // Then update it
        Product updatedProduct = new Product(20, 200, "Updated Product", testSeller.getId());

        mockMvc.perform(put("/api/v1/products/Original Product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productName").value("Updated Product"))
                .andExpect(jsonPath("$.cost").value(200))
                .andExpect(jsonPath("$.amountAvailable").value(20));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void updateProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        Product product = new Product(10, 150, "Non-existent Product", testSeller.getId());

        mockMvc.perform(put("/api/v1/products/NonExistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void deleteProduct_ShouldDeleteProduct_WhenProductExists() throws Exception {
        // First create a product
        Product product = new Product(10, 150, "Product To Delete", testSeller.getId());
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)));

        // Then delete it
        mockMvc.perform(delete("/api/v1/products/Product To Delete"))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/v1/products/NonExistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BUYER")  // Authenticated as BUYER but needs SELLER role
    void deleteProduct_ShouldReturnForbidden_WhenNotSeller() throws Exception {
        mockMvc.perform(delete("/api/v1/products/SomeProduct"))
                .andExpect(status().isForbidden());
    }
}
