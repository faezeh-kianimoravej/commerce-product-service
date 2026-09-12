package com.faezeh.commerce.product.exception;

public class InvalidProductQuantityException extends RuntimeException {

    public InvalidProductQuantityException() {
        super("Quantity must be greater than 0");
    }
}
