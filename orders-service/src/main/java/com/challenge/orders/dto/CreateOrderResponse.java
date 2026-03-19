package com.challenge.orders.dto;

import java.util.List;

/**
 * Respuesta al crear un pedido.
 */
public record CreateOrderResponse(String orderId, String customerId, String status, List<AllocatedItemResponse> allocatedItems) {
}

