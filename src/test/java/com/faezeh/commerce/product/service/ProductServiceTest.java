package com.faezeh.commerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.faezeh.commerce.product.dto.CreateProductRequest;
import com.faezeh.commerce.product.dto.ProductResponse;
import com.faezeh.commerce.product.entity.Product;
import com.faezeh.commerce.product.exception.DuplicateProductSkuException;
import com.faezeh.commerce.product.exception.ProductNotFoundException;
import com.faezeh.commerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductReturnsCreatedProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001",
                "Wireless Mouse",
                "Ergonomic wireless mouse",
                new BigDecimal("29.99"),
                25
        );

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            return product;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.name()).isEqualTo("Wireless Mouse");
        assertThat(response.price()).isEqualByComparingTo("29.99");
        assertThat(response.availableQuantity()).isEqualTo(25);
        assertThat(response.active()).isTrue();
    }

    @Test
    void getProductByIdReturnsExistingProduct() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-001");
    }

    @Test
    void getProductByIdThrowsWhenProductIsMissing() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 99");
    }

    @Test
    void createProductThrowsWhenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001",
                "Wireless Mouse",
                null,
                new BigDecimal("29.99"),
                25
        );
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateProductSkuException.class)
                .hasMessageContaining("SKU-001");
    }

    @Test
    void createProductThrowsWhenSkuExistsEvenIfExistingProductIsInactive() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001",
                "Replacement Mouse",
                null,
                new BigDecimal("39.99"),
                10
        );
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateProductSkuException.class);
    }

    @Test
    void getAllProductsReturnsOnlyActiveProducts() {
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(product()));

        List<ProductResponse> products = productService.getAllProducts();

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().active()).isTrue();
    }

    @Test
    void getProductByIdThrowsWhenProductIsInactive() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 1");
    }

    @Test
    void getProductBySkuThrowsWhenProductIsInactive() {
        when(productRepository.findBySkuAndActiveTrue("SKU-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySku("SKU-001"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("sku: SKU-001");
    }

    @Test
    void updateProductThrowsWhenProductIsInactive() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, null))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 1");
    }

    @Test
    void deleteProductSoftDeletesExistingProduct() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(product);
    }

    private Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setSku("SKU-001");
        product.setName("Wireless Mouse");
        product.setDescription("Ergonomic wireless mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setAvailableQuantity(25);
        product.setActive(true);
        return product;
    }
}
