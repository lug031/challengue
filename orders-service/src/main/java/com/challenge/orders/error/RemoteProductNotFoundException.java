package com.challenge.orders.error;

public class RemoteProductNotFoundException extends RuntimeException {
    private final String productId;

    public RemoteProductNotFoundException(String message, String productId) {
        super(message);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}

