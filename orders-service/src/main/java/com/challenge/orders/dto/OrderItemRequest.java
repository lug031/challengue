package com.challenge.orders.dto;

/**
 * Ítem incluido en la creación de un pedido.
 */
public record OrderItemRequest(String productId, int quantity) {
}

