package com.challenge.orders.error;

public class RemoteInsufficientStockException extends RuntimeException {
    private final String productId;
    private final int requestedQuantity;
    private final int availableStock;

    public RemoteInsufficientStockException(String message, String productId, int requestedQuantity, int availableStock) {
        super(message);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableStock() {
        return availableStock;
    }
}

