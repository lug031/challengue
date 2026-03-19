package com.challenge.common.contracts.inventory;

import java.util.List;

/**
 * Solicitud para asignar inventario a un pedido.
 */
public record AllocateInventoryRequest(String orderId, List<AllocateItemRequest> items) {
}

