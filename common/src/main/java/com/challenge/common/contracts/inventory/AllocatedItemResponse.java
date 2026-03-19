package com.challenge.common.contracts.inventory;

/**
 * Ítem asignado por el Servicio de Inventario.
 */
public record AllocatedItemResponse(String productId, int quantity) {
}

