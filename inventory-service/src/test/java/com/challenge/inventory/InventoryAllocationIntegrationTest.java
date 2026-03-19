package com.challenge.inventory;

import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

/**
 * Prueba de integración del endpoint {@code POST /inventory/allocate} usando H2 en memoria.
 */
@SpringBootTest(
        classes = InventoryApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=test"
)
@AutoConfigureWebTestClient
class InventoryAllocationIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void allocateInventory_whenValidRequest_shouldReturnOk() {
        String productA = "11111111-1111-1111-1111-111111111111";
        String productB = "22222222-2222-2222-2222-222222222222";

        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "order-1",
                List.of(
                        new AllocateItemRequest(productA, 5),
                        new AllocateItemRequest(productB, 1)
                )
        );

        webTestClient.post()
                .uri("/inventory/allocate")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }
}

