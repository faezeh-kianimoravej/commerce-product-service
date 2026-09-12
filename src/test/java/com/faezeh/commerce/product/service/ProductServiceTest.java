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
import com.faezeh.commerce.product.dto.ProductAvailabilityResponse;
import com.faezeh.commerce.product.dto.ProductResponse;
import com.faezeh.commerce.product.dto.UpdateProductRequest;
import com.faezeh.commerce.product.entity.Product;
import com.faezeh.commerce.product.exception.DuplicateProductSkuException;
import com.faezeh.commerce.product.exception.InvalidProductQuantityException;
import com.faezeh.commerce.product.exception.ProductNotFoundException;
import com.faezeh.commerce.product.metrics.ProductMetrics;
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

    @Mock
    private ProductMetrics productMetrics;

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
        verify(productMetrics).productCreated();
    }

    @Test
    void getProductByIdReturnsExistingProduct() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-001");
        verify(productMetrics).productLookup();
    }

    @Test
    void getProductByIdThrowsWhenProductIsMissing() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 99");
        verify(productMetrics, never()).productLookup();
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
        verify(productMetrics, never()).productCreated();
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
        verify(productMetrics, never()).productCreated();
    }

    @Test
    void createProductDoesNotIncrementCounterWhenSaveFails() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001",
                "Wireless Mouse",
                null,
                new BigDecimal("29.99"),
                25
        );
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(productMetrics, never()).productCreated();
    }

    @Test
    void getAllProductsReturnsOnlyActiveProducts() {
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(product()));

        List<ProductResponse> products = productService.getAllProducts();

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().active()).isTrue();
        verify(productMetrics).productList();
    }

    @Test
    void getAllProductsDoesNotIncrementCounterWhenRepositoryFails() {
        when(productRepository.findAllByActiveTrue()).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> productService.getAllProducts())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(productMetrics, never()).productList();
    }

    @Test
    void getProductBySkuReturnsExistingProduct() {
        Product product = product();
        when(productRepository.findBySkuAndActiveTrue("SKU-001")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductBySku("SKU-001");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("SKU-001");
        verify(productMetrics).productLookup();
    }

    @Test
    void getProductByIdThrowsWhenProductIsInactive() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 1");
        verify(productMetrics, never()).productLookup();
    }

    @Test
    void getProductBySkuThrowsWhenProductIsInactive() {
        when(productRepository.findBySkuAndActiveTrue("SKU-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySku("SKU-001"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("sku: SKU-001");
        verify(productMetrics, never()).productLookup();
    }

    @Test
    void checkAvailabilityReturnsAvailableWhenRequestedQuantityIsInStock() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        ProductAvailabilityResponse response = productService.checkAvailability(1L, 10);

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.existsAndActive()).isTrue();
        assertThat(response.available()).isTrue();
        assertThat(response.availableQuantity()).isEqualTo(25);
    }

    @Test
    void checkAvailabilityReturnsUnavailableWhenRequestedQuantityExceedsStock() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        ProductAvailabilityResponse response = productService.checkAvailability(1L, 30);

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.existsAndActive()).isTrue();
        assertThat(response.available()).isFalse();
        assertThat(response.availableQuantity()).isEqualTo(25);
    }

    @Test
    void checkAvailabilityReturnsUnavailableWhenProductIsMissingOrInactive() {
        when(productRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        ProductAvailabilityResponse response = productService.checkAvailability(99L, 1);

        assertThat(response.productId()).isEqualTo(99L);
        assertThat(response.existsAndActive()).isFalse();
        assertThat(response.available()).isFalse();
        assertThat(response.availableQuantity()).isZero();
    }

    @Test
    void checkAvailabilityThrowsWhenQuantityIsNotPositive() {
        assertThatThrownBy(() -> productService.checkAvailability(1L, 0))
                .isInstanceOf(InvalidProductQuantityException.class)
                .hasMessage("Quantity must be greater than 0");
        verify(productRepository, never()).findByIdAndActiveTrue(1L);
    }

    @Test
    void updateProductReturnsUpdatedProduct() {
        Product product = product();
        UpdateProductRequest request = new UpdateProductRequest(
                "Gaming Mouse",
                "Fast wireless mouse",
                new BigDecimal("49.99"),
                12,
                true
        );
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.updateProduct(1L, request);

        assertThat(response.name()).isEqualTo("Gaming Mouse");
        assertThat(response.description()).isEqualTo("Fast wireless mouse");
        assertThat(response.price()).isEqualByComparingTo("49.99");
        assertThat(response.availableQuantity()).isEqualTo(12);
        assertThat(response.active()).isTrue();
        verify(productMetrics).productUpdated();
    }

    @Test
    void updateProductThrowsWhenProductIsInactive() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(1L, null))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 1");
        verify(productMetrics, never()).productUpdated();
    }

    @Test
    void updateProductDoesNotIncrementCounterWhenSaveFails() {
        Product product = product();
        UpdateProductRequest request = new UpdateProductRequest(
                "Gaming Mouse",
                "Fast wireless mouse",
                new BigDecimal("49.99"),
                12,
                true
        );
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> productService.updateProduct(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(productMetrics, never()).productUpdated();
    }

    @Test
    void deleteProductSoftDeletesExistingProduct() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(product);
        verify(productMetrics).productDeleted();
    }

    @Test
    void deleteProductDoesNotIncrementCounterWhenProductIsInactive() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("id: 1");
        verify(productMetrics, never()).productDeleted();
    }

    @Test
    void deleteProductDoesNotIncrementCounterWhenSaveFails() {
        Product product = product();
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> productService.deleteProduct(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");
        verify(productMetrics, never()).productDeleted();
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
