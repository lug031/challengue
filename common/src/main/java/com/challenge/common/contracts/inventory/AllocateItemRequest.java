package com.challenge.common.contracts.inventory;

/**
 * Ítem a asignar (producto y cantidad).
 */
public record AllocateItemRequest(String productId, int quantity) {
}

