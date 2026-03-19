package com.challenge.orders.service;

import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.orders.domain.Order;
import com.challenge.orders.domain.OrderItem;
import com.challenge.orders.client.InventoryClient;
import com.challenge.orders.dto.AllocatedItemResponse;
import com.challenge.orders.dto.CreateOrderRequest;
import com.challenge.orders.dto.CreateOrderResponse;
import com.challenge.orders.dto.GetOrderResponse;
import com.challenge.orders.dto.OrderItemRequest;
import com.challenge.orders.error.OrderInvalidRequestException;
import com.challenge.orders.error.OrderNotFoundException;
import com.challenge.orders.error.RemoteInsufficientStockException;
import com.challenge.orders.error.RemoteProductNotFoundException;
import com.challenge.orders.error.RemoteUnavailableException;
import com.challenge.orders.repository.OrderItemRepository;
import com.challenge.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del Servicio de Pedidos, incluyendo propagación de errores desde Inventario.
 */
@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @Mock
    InventoryClient inventoryClient;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @InjectMocks
    DefaultOrdersService ordersService;

    @Test
    void createOrder_success_whenInventoryAllocates() {
        String customerId = "cust-1";
        String productA = "11111111-1111-1111-1111-111111111111";
        String productB = "22222222-2222-2222-2222-222222222222";

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(
                        new OrderItemRequest(productA, 2),
                        new OrderItemRequest(productA, 1), // duplicado intencional para consolidacion
                        new OrderItemRequest(productB, 3)
                )
        );

        AllocateInventoryResponse remoteResponse = new AllocateInventoryResponse(
                "ignored",
                List.of(
                        new com.challenge.common.contracts.inventory.AllocatedItemResponse(productA, 3),
                        new com.challenge.common.contracts.inventory.AllocatedItemResponse(productB, 3)
                )
        );

        when(inventoryClient.allocateInventory(any(), any()))
                .thenReturn(Mono.just(remoteResponse));

        when(orderRepository.insert(any(Order.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(orderItemRepository.insert(any(OrderItem.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<CreateOrderResponse> result = ordersService.createOrder(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.customerId()).isEqualTo(customerId);
                    assertThat(response.status()).isEqualTo("CONFIRMED");
                    assertThat(response.allocatedItems()).hasSize(2);
                    assertThat(response.allocatedItems()).extracting(AllocatedItemResponse::productId)
                            .containsExactlyInAnyOrder(productA, productB);
                })
                .verifyComplete();
    }

    @Test
    void createOrder_error_whenRequestInvalid() {
        CreateOrderRequest request = new CreateOrderRequest(
                "   ",
                List.of(new OrderItemRequest("11111111-1111-1111-1111-111111111111", 1))
        );

        StepVerifier.create(ordersService.createOrder(request))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(OrderInvalidRequestException.class))
                .verify();

        verifyNoInteractions(inventoryClient, orderRepository, orderItemRepository);
    }

    @Test
    void createOrder_error_whenRemoteProductNotFound() {
        String customerId = "cust-1";
        String missingProduct = "33333333-3333-3333-3333-333333333333";

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest(missingProduct, 1))
        );

        when(inventoryClient.allocateInventory(any(), any()))
                .thenReturn(Mono.error(new RemoteProductNotFoundException("Producto no encontrado", missingProduct)));

        StepVerifier.create(ordersService.createOrder(request))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(RemoteProductNotFoundException.class))
                .verify();

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    @Test
    void createOrder_error_whenRemoteInsufficientStock() {
        String customerId = "cust-1";
        String productId = "11111111-1111-1111-1111-111111111111";

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest(productId, 10))
        );

        when(inventoryClient.allocateInventory(any(), any()))
                .thenReturn(Mono.error(new RemoteInsufficientStockException("Stock insuficiente", productId, 10, 2)));

        StepVerifier.create(ordersService.createOrder(request))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(RemoteInsufficientStockException.class))
                .verify();

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    @Test
    void createOrder_error_whenRemoteUnavailable() {
        String customerId = "cust-1";
        String productId = "11111111-1111-1111-1111-111111111111";

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                List.of(new OrderItemRequest(productId, 1))
        );

        when(inventoryClient.allocateInventory(any(), any()))
                .thenReturn(Mono.error(new RemoteUnavailableException("No disponible")));

        StepVerifier.create(ordersService.createOrder(request))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(RemoteUnavailableException.class))
                .verify();

        verifyNoInteractions(orderRepository, orderItemRepository);
    }

    @Test
    void getOrderById_success_whenOrderExists() {
        String orderId = "order-1";

        when(orderRepository.findById(orderId))
                .thenReturn(Mono.just(new Order(orderId, "cust-1", "CONFIRMED")));

        when(orderItemRepository.findByOrderId(orderId))
                .thenReturn(Flux.just(
                        new OrderItem("item-1", orderId, "11111111-1111-1111-1111-111111111111", 2),
                        new OrderItem("item-2", orderId, "22222222-2222-2222-2222-222222222222", 1)
                ));

        StepVerifier.create(ordersService.getOrderById(orderId))
                .assertNext(resp -> {
                    assertThat(resp).isInstanceOf(GetOrderResponse.class);
                    assertThat(resp.orderId()).isEqualTo(orderId);
                    assertThat(resp.customerId()).isEqualTo("cust-1");
                    assertThat(resp.status()).isEqualTo("CONFIRMED");
                    assertThat(resp.items()).hasSize(2);
                })
                .verifyComplete();

        verify(orderRepository).findById(eq(orderId));
        verify(orderItemRepository).findByOrderId(eq(orderId));
    }

    @Test
    void getOrderById_error_whenOrderMissing() {
        String orderId = "missing-order";

        when(orderRepository.findById(orderId))
                .thenReturn(Mono.empty());

        StepVerifier.create(ordersService.getOrderById(orderId))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(OrderNotFoundException.class))
                .verify();

        verify(orderRepository).findById(eq(orderId));
        verifyNoInteractions(orderItemRepository);
    }
}

