package com.challenge.orders.error;

public class OrderInvalidRequestException extends RuntimeException {
    public OrderInvalidRequestException(String message) {
        super(message);
    }
}

