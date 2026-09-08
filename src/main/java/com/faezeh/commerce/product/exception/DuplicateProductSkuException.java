package com.faezeh.commerce.product.exception;

public class DuplicateProductSkuException extends RuntimeException {

    public DuplicateProductSkuException(String sku) {
        super("Product already exists with SKU: " + sku);
    }
}
