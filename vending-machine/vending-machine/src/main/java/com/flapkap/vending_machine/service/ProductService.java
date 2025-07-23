package com.flapkap.vending_machine.service;

import com.flapkap.vending_machine.dto.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProductService {
    List<Product> getAllProducts();

    Product createProduct(Product product);

    Product updateProduct(String productName, Product product);

    void deleteProduct(String productName);
}
