package com.challenge.orders.dto;

import java.util.List;

/**
 * Solicitud para crear un pedido.
 */
public record CreateOrderRequest(String customerId, List<OrderItemRequest> items) {
}

