package com.faezeh.commerce.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String field, Object value) {
        super("Product not found with " + field + ": " + value);
    }
}
