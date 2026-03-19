package com.challenge.orders.dto;

import java.util.List;

/**
 * Respuesta al consultar un pedido por id.
 */
public record GetOrderResponse(String orderId, String customerId, String status, List<AllocatedItemResponse> items) {
}

