package com.challenge.inventory.error;

public class ProductNotFoundException extends RuntimeException {
    private final String productId;

    public ProductNotFoundException(String message, String productId) {
        super(message);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}

