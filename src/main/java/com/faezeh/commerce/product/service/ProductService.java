package com.faezeh.commerce.product.service;

import java.util.List;

import com.faezeh.commerce.product.dto.CreateProductRequest;
import com.faezeh.commerce.product.dto.ProductResponse;
import com.faezeh.commerce.product.dto.UpdateProductRequest;
import com.faezeh.commerce.product.entity.Product;
import com.faezeh.commerce.product.exception.DuplicateProductSkuException;
import com.faezeh.commerce.product.exception.ProductNotFoundException;
import com.faezeh.commerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateProductSkuException(request.sku());
        }

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailableQuantity(request.availableQuantity());
        product.setActive(true);

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return toResponse(findProductById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        return productRepository.findBySkuAndActiveTrue(sku)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException("sku", sku));
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = findProductById(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setAvailableQuantity(request.availableQuantity());
        product.setActive(request.active());

        return toResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException("id", id));
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
}
