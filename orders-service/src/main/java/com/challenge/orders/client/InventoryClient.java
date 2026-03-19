package com.challenge.orders.client;

import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Cliente HTTP reactivo para comunicarse con el Servicio de Inventario.
 */
public interface InventoryClient {
    /**
     * Envía una solicitud de asignación de inventario para los ítems de un pedido.
     */
    Mono<AllocateInventoryResponse> allocateInventory(String orderId, List<AllocateItemRequest> items);
}

