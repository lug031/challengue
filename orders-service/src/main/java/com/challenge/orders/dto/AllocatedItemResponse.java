package com.challenge.orders.dto;

/**
 * Ítem asignado dentro de la respuesta del pedido.
 */
public record AllocatedItemResponse(String productId, int quantity) {
}

