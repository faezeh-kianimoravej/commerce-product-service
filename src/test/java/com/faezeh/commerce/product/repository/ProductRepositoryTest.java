package com.faezeh.commerce.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.faezeh.commerce.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findBySkuReturnsProduct() {
        Product savedProduct = productRepository.saveAndFlush(product("SKU-001"));

        assertThat(productRepository.findBySku("SKU-001"))
                .isPresent()
                .get()
                .extracting(Product::getId)
                .isEqualTo(savedProduct.getId());
    }

    @Test
    void activeQueriesExcludeInactiveProducts() {
        Product activeProduct = product("SKU-ACTIVE");
        Product inactiveProduct = product("SKU-INACTIVE");
        inactiveProduct.setActive(false);

        productRepository.saveAndFlush(activeProduct);
        productRepository.saveAndFlush(inactiveProduct);

        assertThat(productRepository.findAllByActiveTrue())
                .extracting(Product::getSku)
                .containsOnly("SKU-ACTIVE");
        assertThat(productRepository.findByIdAndActiveTrue(inactiveProduct.getId())).isEmpty();
        assertThat(productRepository.findBySkuAndActiveTrue("SKU-INACTIVE")).isEmpty();
    }

    @Test
    void inactiveProductRemainsPersistedAndCanBeLoadedByRegularIdLookup() {
        Product inactiveProduct = product("SKU-SOFT-DELETED");
        inactiveProduct.setActive(false);

        Product savedProduct = productRepository.saveAndFlush(inactiveProduct);

        assertThat(productRepository.findById(savedProduct.getId()))
                .isPresent()
                .get()
                .extracting(Product::getActive)
                .isEqualTo(false);
    }

    @Test
    void existsBySkuReturnsTrueWhenProductExists() {
        productRepository.saveAndFlush(product("SKU-002"));

        assertThat(productRepository.existsBySku("SKU-002")).isTrue();
        assertThat(productRepository.existsBySku("MISSING")).isFalse();
    }

    @Test
    void existsBySkuReturnsTrueForInactiveProduct() {
        Product product = product("SKU-INACTIVE-DUPLICATE");
        product.setActive(false);
        productRepository.saveAndFlush(product);

        assertThat(productRepository.existsBySku("SKU-INACTIVE-DUPLICATE")).isTrue();
    }

    @Test
    void timestampsUseInstant() {
        Product savedProduct = productRepository.saveAndFlush(product("SKU-003"));

        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();
    }

    private Product product(String sku) {
        Product product = new Product();
        product.setSku(sku);
        product.setName("Wireless Mouse");
        product.setDescription("Ergonomic wireless mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setAvailableQuantity(25);
        product.setActive(true);
        return product;
    }
}
