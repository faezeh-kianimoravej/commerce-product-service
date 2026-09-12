package com.faezeh.commerce.product.service;

import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMetrics productMetrics;

    public ProductService(ProductRepository productRepository, ProductMetrics productMetrics) {
        this.productRepository = productRepository;
        this.productMetrics = productMetrics;
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            LOGGER.warn("Product creation rejected due to duplicate sku: sku={}", request.sku());
            throw new DuplicateProductSkuException(request.sku());
        }

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailableQuantity(request.availableQuantity());
        product.setActive(true);

        Product savedProduct = productRepository.save(product);
        productMetrics.productCreated();
        LOGGER.info("Product created: id={}, sku={}", savedProduct.getId(), savedProduct.getSku());
        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<ProductResponse> products = productRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
        productMetrics.productList();
        return products;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductById(id);
        productMetrics.productLookup();
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> {
                    LOGGER.warn("Product not found: sku={}", sku);
                    return new ProductNotFoundException("sku", sku);
                });
        productMetrics.productLookup();
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductAvailabilityResponse checkAvailability(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidProductQuantityException();
        }

        return productRepository.findByIdAndActiveTrue(productId)
                .map(product -> toAvailabilityResponse(product, quantity))
                .orElseGet(() -> new ProductAvailabilityResponse(productId, false, false, 0));
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findProductById(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailableQuantity(request.availableQuantity());
        product.setActive(request.active());

        Product savedProduct = productRepository.save(product);
        productMetrics.productUpdated();
        LOGGER.info("Product updated: id={}, sku={}", savedProduct.getId(), savedProduct.getSku());
        return toResponse(savedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        product.setActive(false);
        productRepository.save(product);
        productMetrics.productDeleted();
        LOGGER.info("Product soft-deleted: id={}, sku={}", product.getId(), product.getSku());
    }

    private Product findProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> {
                    LOGGER.warn("Product not found: id={}", id);
                    return new ProductNotFoundException("id", id);
                });
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getAvailableQuantity(),
                product.getActive()
        );
    }

    private ProductAvailabilityResponse toAvailabilityResponse(Product product, int quantity) {
        int availableQuantity = product.getAvailableQuantity();
        return new ProductAvailabilityResponse(
                product.getId(),
                true,
                availableQuantity >= quantity,
                availableQuantity
        );
    }
}
