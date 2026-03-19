package com.challenge.inventory.controller;

import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import com.challenge.common.contracts.inventory.AllocatedItemResponse;
import com.challenge.inventory.error.InventoryExceptionHandler;
import com.challenge.inventory.error.ProductNotFoundException;
import com.challenge.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas de contrato del endpoint {@code POST /inventory/allocate}.
 */
@WebFluxTest(controllers = InventoryController.class)
@Import(InventoryExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    InventoryService inventoryService;

    @Test
    void allocate_returns200_whenServiceOk() {
        AllocateInventoryResponse response = new AllocateInventoryResponse(
                "order-1",
                List.of(new AllocatedItemResponse("11111111-1111-1111-1111-111111111111", 3))
        );

        when(inventoryService.allocateInventory(any()))
                .thenReturn(Mono.just(response));

        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "order-1",
                List.of(new AllocateItemRequest("11111111-1111-1111-1111-111111111111", 3))
        );

        webTestClient.post()
                .uri("/inventory/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.allocatedItems[0].productId").isEqualTo("11111111-1111-1111-1111-111111111111")
                .jsonPath("$.allocatedItems[0].quantity").isEqualTo(3);
    }

    @Test
    void allocate_returns404_whenProductNotFound() {
        String productId = "33333333-3333-3333-3333-333333333333";

        when(inventoryService.allocateInventory(any()))
                .thenReturn(Mono.error(new ProductNotFoundException("Producto no encontrado", productId)));

        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "order-1",
                List.of(new AllocateItemRequest(productId, 1))
        );

        webTestClient.post()
                .uri("/inventory/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }
}

