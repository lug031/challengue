package com.challenge.inventory.error;

public class InventoryInvalidRequestException extends RuntimeException {
    public InventoryInvalidRequestException(String message) {
        super(message);
    }
}

