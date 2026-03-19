package com.challenge.orders.controller;

import com.challenge.orders.dto.AllocatedItemResponse;
import com.challenge.orders.dto.CreateOrderRequest;
import com.challenge.orders.dto.CreateOrderResponse;
import com.challenge.orders.dto.GetOrderResponse;
import com.challenge.orders.dto.OrderItemRequest;
import com.challenge.orders.error.OrderNotFoundException;
import com.challenge.orders.error.OrdersExceptionHandler;
import com.challenge.orders.error.RemoteProductNotFoundException;
import com.challenge.orders.service.OrdersService;
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
 * Pruebas de contrato del endpoint {@code POST /orders}.
 */
@WebFluxTest(controllers = OrdersController.class)
@Import(OrdersExceptionHandler.class)
class OrdersControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    OrdersService ordersService;

    @Test
    void createOrder_returns201_whenServiceOk() {
        CreateOrderResponse response = new CreateOrderResponse(
                "order-1",
                "cust-1",
                "CONFIRMED",
                List.of(new AllocatedItemResponse("11111111-1111-1111-1111-111111111111", 3))
        );

        when(ordersService.createOrder(any()))
                .thenReturn(Mono.just(response));

        CreateOrderRequest request = new CreateOrderRequest(
                "cust-1",
                List.of(new OrderItemRequest("11111111-1111-1111-1111-111111111111", 3))
        );

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.customerId").isEqualTo("cust-1")
                .jsonPath("$.status").isEqualTo("CONFIRMED")
                .jsonPath("$.allocatedItems[0].productId").isEqualTo("11111111-1111-1111-1111-111111111111")
                .jsonPath("$.allocatedItems[0].quantity").isEqualTo(3);
    }

    @Test
    void createOrder_returns404_whenRemoteProductNotFound() {
        String missingProduct = "33333333-3333-3333-3333-333333333333";

        when(ordersService.createOrder(any()))
                .thenReturn(Mono.error(new RemoteProductNotFoundException("Producto no encontrado", missingProduct)));

        CreateOrderRequest request = new CreateOrderRequest(
                "cust-1",
                List.of(new OrderItemRequest(missingProduct, 1))
        );

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void getOrder_returns200_whenOrderExists() {
        GetOrderResponse response = new GetOrderResponse(
                "order-1",
                "cust-1",
                "CONFIRMED",
                List.of(new AllocatedItemResponse("11111111-1111-1111-1111-111111111111", 3))
        );

        when(ordersService.getOrderById("order-1"))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/orders/order-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.customerId").isEqualTo("cust-1")
                .jsonPath("$.status").isEqualTo("CONFIRMED")
                .jsonPath("$.items[0].productId").isEqualTo("11111111-1111-1111-1111-111111111111")
                .jsonPath("$.items[0].quantity").isEqualTo(3);
    }

    @Test
    void getOrder_returns404_whenOrderMissing() {
        when(ordersService.getOrderById("missing-order"))
                .thenReturn(Mono.error(new OrderNotFoundException("Pedido no encontrado", "missing-order")));

        webTestClient.get()
                .uri("/orders/missing-order")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ORDER_NOT_FOUND");
    }
}

