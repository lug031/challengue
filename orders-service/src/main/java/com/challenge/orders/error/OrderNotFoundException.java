package com.challenge.orders.error;

public class OrderNotFoundException extends RuntimeException {

    private final String orderId;

    public OrderNotFoundException(String message, String orderId) {
        super(message);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}

