package com.challenge.inventory.controller;

import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.inventory.service.InventoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Endpoints del Servicio de Inventario.
 */
@RestController
@RequestMapping(path = "/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Asigna inventario a los ítems de un pedido.
     */
    @PostMapping(path = "/allocate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AllocateInventoryResponse> allocate(@RequestBody Mono<AllocateInventoryRequest> request) {
        return request.flatMap(inventoryService::allocateInventory);
    }
}

