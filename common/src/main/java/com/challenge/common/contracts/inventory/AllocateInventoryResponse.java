package com.challenge.common.contracts.inventory;

import java.util.List;

/**
 * Respuesta con los ítems asignados para un pedido.
 */
public record AllocateInventoryResponse(String orderId, List<AllocatedItemResponse> allocatedItems) {
}

