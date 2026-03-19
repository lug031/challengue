package com.challenge.inventory.service;

import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import reactor.core.publisher.Mono;

/**
 * Reglas de negocio del Servicio de Inventario.
 */
public interface InventoryService {
    /**
     * Asigna inventario para los ítems de un pedido.
     */
    Mono<AllocateInventoryResponse> allocateInventory(AllocateInventoryRequest request);
}

