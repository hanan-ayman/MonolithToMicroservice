package com.flapkap.vending_machine.service.impl;

import com.flapkap.vending_machine.dto.Product;
import com.flapkap.vending_machine.exception.ResourceNotFoundException;
import com.flapkap.vending_machine.entity.ProductEntity;
import com.flapkap.vending_machine.entity.UserEntity;
import com.flapkap.vending_machine.repository.ProductRepository;
import com.flapkap.vending_machine.repository.UserRepository;
import com.flapkap.vending_machine.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.info("Fetching all products");
        try {
            List<Product> products = productRepository.findAll().stream()
                    .map(this::convertEntityToDto)
                    .collect(Collectors.toList());
            log.info("Successfully retrieved {} products", products.size());
            return products;
        } catch (Exception e) {
            log.error("Error fetching all products", e);
            throw e;
        }
    }

    @Override
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.productName());
        try {
            ProductEntity entity = convertDtoToEntity(product);
            ProductEntity savedEntity = productRepository.save(entity);
            Product result = convertEntityToDto(savedEntity);
            log.info("Successfully created product with ID: {} and name: {}", savedEntity.getId(), result.productName());
            return result;
        } catch (Exception e) {
            log.error("Error creating product: {}", product.productName(), e);
            throw e;
        }
    }

    @Override
    public Product updateProduct(String productName, Product product) {
        log.info("Updating product: {}", productName);
        try {
            ProductEntity existingEntity = productRepository.findByProductName(productName)
                    .orElseThrow(() -> {
                        log.warn("Product not found for update: {}", productName);
                        return new ResourceNotFoundException("Product not found with name: " + productName);
                    });

            existingEntity.setAmountAvailable(product.amountAvailable());
            existingEntity.setCost(product.cost());
            existingEntity.setProductName(product.productName());

            if (product.sellerId() != null) {
                UserEntity seller = userRepository.findById(product.sellerId())
                        .orElseThrow(() -> {
                            log.warn("Seller not found with ID: {} for product: {}", product.sellerId(), productName);
                            return new ResourceNotFoundException("Seller not found with id: " + product.sellerId());
                        });
                existingEntity.setSellerId(seller);
            }

            ProductEntity updatedEntity = productRepository.save(existingEntity);
            Product result = convertEntityToDto(updatedEntity);
            log.info("Successfully updated product: {}", productName);
            return result;
        } catch (Exception e) {
            log.error("Error updating product: {}", productName, e);
            throw e;
        }
    }

    @Override
    public void deleteProduct(String productName) {
        log.info("Deleting product: {}", productName);
        if (productName == null) {
            log.error("Product name cannot be null for deletion");
            throw new IllegalArgumentException("Product name cannot be null");
        }

        try {
            if (!productRepository.existsByProductName(productName)) {
                log.warn("Product not found for deletion: {}", productName);
                throw new ResourceNotFoundException("Product not found with name: " + productName);
            }

            productRepository.deleteByProductName(productName);
            log.info("Successfully deleted product: {}", productName);
        } catch (Exception e) {
            log.error("Error deleting product: {}", productName, e);
            throw e;
        }
    }

    private Product convertEntityToDto(ProductEntity entity) {
        return new Product(
                entity.getAmountAvailable(),
                entity.getCost(),
                entity.getProductName(),
                entity.getSellerId().getId()
        );
    }

    private ProductEntity convertDtoToEntity(Product dto) {
        UserEntity seller = userRepository.findById(dto.sellerId())
                .orElseThrow(() -> {
                    log.warn("Seller not found with ID: {} for product creation", dto.sellerId());
                    return new ResourceNotFoundException("Seller not found with id: " + dto.sellerId());
                });

        ProductEntity entity = new ProductEntity();
        entity.setAmountAvailable(dto.amountAvailable());
        entity.setCost(dto.cost());
        entity.setProductName(dto.productName());
        entity.setSellerId(seller);
        return entity;
    }
}
