package com.challenge.orders;

import com.challenge.common.api.ApiError;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.orders.dto.AllocatedItemResponse;
import com.challenge.orders.dto.CreateOrderRequest;
import com.challenge.orders.dto.CreateOrderResponse;
import com.challenge.orders.dto.OrderItemRequest;
import com.challenge.inventory.InventoryApplication;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

/**
 * Prueba de integración que levanta ambos microservicios y valida el flujo {@code Orders -> Inventory}.
 * Además cubre respuestas 201, 404, 409 y 503 según el escenario.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdersInventoryIntegrationTest {

    private static ReactiveWebServerApplicationContext inventoryContext;
    private static ReactiveWebServerApplicationContext ordersContext;
    private static int inventoryPort;
    private static int ordersPort;

    private static WebTestClient webTestClient;

    @BeforeAll
    static void startBothServices() {
        // Asegurar que init SQL y repositorio usan exactamente la msma BD en memoria
        String inventoryDbName = "inventorydb-it-" + UUID.randomUUID().toString().replace("-", "");
        String ordersDbName = "ordersdb-it-" + UUID.randomUUID().toString().replace("-", "");

        inventoryContext = (ReactiveWebServerApplicationContext) new SpringApplicationBuilder(InventoryApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.profiles.active=test",
                        "--spring.r2dbc.url=r2dbc:h2:mem:///" + inventoryDbName + ";DB_CLOSE_DELAY=-1",
                        "--spring.r2dbc.generate-unique-name=false"
                );

        inventoryPort = inventoryContext.getWebServer().getPort();

        String sqlInitMode = inventoryContext.getEnvironment().getProperty("spring.sql.init.mode");
        Assertions.assertThat(sqlInitMode).isEqualTo("always");

        WebTestClient inventoryClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + inventoryPort)
                .build();

        // Request inválido esperado 400
        inventoryClient.post()
                .uri("/inventory/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AllocateInventoryRequest("ping", List.of()))
                .exchange()
                .expectStatus().isBadRequest();

        // Request válido esperado 200
        String productA = "11111111-1111-1111-1111-111111111111";
        String productB = "22222222-2222-2222-2222-222222222222";

        AllocateInventoryRequest seedRequest = new AllocateInventoryRequest(
                "seed-order",
                List.of(
                        new AllocateItemRequest(productA, 5),
                        new AllocateItemRequest(productB, 1)
                )
        );
        assertInventoryAllocateOkWithRetry(inventoryClient, seedRequest, 5);

        // validar el flujo Orders - Inventory.
        ordersContext = (ReactiveWebServerApplicationContext) new SpringApplicationBuilder(OrdersApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.profiles.active=test",
                        "--spring.r2dbc.url=r2dbc:h2:mem:///" + ordersDbName + ";DB_CLOSE_DELAY=-1",
                        "--spring.r2dbc.generate-unique-name=false",
                        "--orders.inventory.base-url=http://localhost:" + inventoryPort
                );

        ordersPort = ordersContext.getWebServer().getPort();

        String configuredBaseUrl = ordersContext.getEnvironment().getProperty("orders.inventory.base-url");
        Assertions.assertThat(configuredBaseUrl).isEqualTo("http://localhost:" + inventoryPort);

        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + ordersPort)
                .build();
    }

    private static void assertInventoryAllocateOkWithRetry(WebTestClient inventoryClient,
                                                              AllocateInventoryRequest request,
                                                              int maxAttempts) {
        AssertionError last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                inventoryClient.post()
                        .uri("/inventory/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isOk();
                return;
            } catch (AssertionError ex) {
                last = ex;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(400L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }
        throw last;
    }

    @AfterAll
    static void stopBothServices() {
        if (ordersContext != null) {
            ordersContext.close();
        }
        if (inventoryContext != null) {
            inventoryContext.close();
        }
    }

    @Test
    @Order(1)
    void createOrder_success_shouldAllocateAndConfirm() {
        String customerId = "cust-1";
        String productA = "11111111-1111-1111-1111-111111111111";
        String productB = "22222222-2222-2222-2222-222222222222";

        // Incluimos duplicados para validar normalizacion por productId
        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(
                        new OrderItemRequest(productA, 2),
                        new OrderItemRequest(productA, 3),
                        new OrderItemRequest(productB, 1)
                )
        );

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateOrderResponse.class)
                .value(resp -> {
                    Assertions.assertThat(resp.status()).isEqualTo("CONFIRMED");
                    Assertions.assertThat(resp.customerId()).isEqualTo(customerId);
                    Assertions.assertThat(resp.allocatedItems()).hasSize(2);

                    Assertions.assertThat(resp.allocatedItems())
                            .extracting(AllocatedItemResponse::productId, AllocatedItemResponse::quantity)
                            .containsExactlyInAnyOrder(
                                    org.assertj.core.groups.Tuple.tuple(productA, 5),
                                    org.assertj.core.groups.Tuple.tuple(productB, 1)
                            );
                });
    }

    @Test
    @Order(2)
    void createOrder_error_whenInsufficientStock_shouldReturnApiError() {
        String customerId = "cust-2";
        String productB = "22222222-2222-2222-2222-222222222222";

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(
                        // Producto B inicia con stock=3
                        new OrderItemRequest(productB, 5)
                )
        );

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ApiError.class)
                .value(error -> {
                    Assertions.assertThat(error.code()).isEqualTo("INSUFFICIENT_STOCK");
                    Assertions.assertThat(error.details()).hasSize(3);
                    Assertions.assertThat(error.details().get(0)).isEqualTo(productB);
                    Assertions.assertThat(error.details().get(1)).isEqualTo("5");
                    int available = Integer.parseInt(error.details().get(2));
                    Assertions.assertThat(available).isBetween(1, 2);
                });
    }

    @Test
    @Order(3)
    void createOrder_error_whenProductNotFound_shouldReturnApiError404() {
        String customerId = "cust-404";
        String missingProduct = "33333333-3333-3333-3333-333333333333";

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(
                        new OrderItemRequest(missingProduct, 1)
                )
        );

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody(ApiError.class)
                .value(error -> {
                    Assertions.assertThat(error.code()).isEqualTo("PRODUCT_NOT_FOUND");
                    Assertions.assertThat(error.details()).hasSize(1);
                    Assertions.assertThat(error.details().get(0)).isEqualTo(missingProduct);
                });
    }

    @Test
    @Order(4)
    void createOrder_error_whenRemoteUnavailable_shouldReturnApiError503() {
        String ordersDbName = "ordersdb-unavail-it-" + UUID.randomUUID().toString().replace("-", "");

        ReactiveWebServerApplicationContext unavailableOrdersContext =
                (ReactiveWebServerApplicationContext) new SpringApplicationBuilder(OrdersApplication.class)
                        .run(
                                "--server.port=0",
                                "--spring.profiles.active=test",
                                "--spring.r2dbc.url=r2dbc:h2:mem:///" + ordersDbName + ";DB_CLOSE_DELAY=-1",
                                "--spring.r2dbc.generate-unique-name=false",
                                "--orders.inventory.base-url=http://localhost:1"
                        );

        int unavailableOrdersPort = unavailableOrdersContext.getWebServer().getPort();

        WebTestClient unavailableClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + unavailableOrdersPort)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "cust-503",
                List.of(new OrderItemRequest("11111111-1111-1111-1111-111111111111", 1))
        );

        unavailableClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(ApiError.class)
                .value(error -> {
                    Assertions.assertThat(error.code()).isEqualTo("REMOTE_UNAVAILABLE");
                    Assertions.assertThat(error.details()).isEmpty();
                });
    }
}

