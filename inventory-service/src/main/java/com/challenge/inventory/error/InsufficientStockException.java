package com.challenge.inventory.error;

public class InsufficientStockException extends RuntimeException {
    private final String productId;
    private final int availableStock;
    private final int requestedQuantity;

    public InsufficientStockException(String message, String productId, int availableStock, int requestedQuantity) {
        super(message);
        this.productId = productId;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}

