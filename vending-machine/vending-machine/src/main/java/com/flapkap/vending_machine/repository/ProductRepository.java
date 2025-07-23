package com.flapkap.vending_machine.repository;

import com.flapkap.vending_machine.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByProductName(String productName);
    boolean existsByProductName(String productName);
    void deleteByProductName(String productName);
}
