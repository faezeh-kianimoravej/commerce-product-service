package com.faezeh.commerce.product.repository;

import java.util.List;
import java.util.Optional;

import com.faezeh.commerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findAllByActiveTrue();

    Optional<Product> findByIdAndActiveTrue(Long id);

    Optional<Product> findBySkuAndActiveTrue(String sku);
}
