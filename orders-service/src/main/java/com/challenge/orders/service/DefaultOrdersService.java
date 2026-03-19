package com.challenge.orders.service;

import com.challenge.common.contracts.inventory.AllocateItemRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.orders.client.InventoryClient;
import com.challenge.orders.domain.Order;
import com.challenge.orders.domain.OrderItem;
import com.challenge.orders.dto.AllocatedItemResponse;
import com.challenge.orders.dto.CreateOrderRequest;
import com.challenge.orders.dto.CreateOrderResponse;
import com.challenge.orders.dto.GetOrderResponse;
import com.challenge.orders.dto.OrderItemRequest;
import com.challenge.orders.error.OrderInvalidRequestException;
import com.challenge.orders.error.OrderNotFoundException;
import com.challenge.orders.repository.OrderItemRepository;
import com.challenge.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementación reactiva del Servicio de Pedidos.
 */
@Service
public class DefaultOrdersService implements OrdersService {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrdersService.class);
    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public DefaultOrdersService(InventoryClient inventoryClient,
                                  OrderRepository orderRepository,
                                  OrderItemRepository orderItemRepository) {
        this.inventoryClient = inventoryClient;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public Mono<CreateOrderResponse> createOrder(CreateOrderRequest request) {
        // Valida el request y normaliza los ítems antes de persistir.
        Supplier<OrderInvalidRequestException> invalidRequestSupplier = () ->
                new OrderInvalidRequestException("El request de creacion de pedido es invalido");

        Predicate<String> isNonBlank = value ->
                Optional.ofNullable(value).map(String::trim).filter(v -> !v.isBlank()).isPresent();

        Predicate<OrderItemRequest> isValidItem = item ->
                item != null
                        && isNonBlank.test(item.productId())
                        && item.quantity() > 0;

        Optional<String> customerIdOpt = Optional.ofNullable(request)
                .map(CreateOrderRequest::customerId)
                .map(s -> s == null ? null : s.trim())
                .filter(isNonBlank);

        if (customerIdOpt.isEmpty()) {
            return Mono.error(invalidRequestSupplier.get());
        }

        String customerId = customerIdOpt.get();

        List<OrderItemRequest> items = Optional.ofNullable(request.items()).orElse(List.of());
        if (items.isEmpty() || items.stream().anyMatch(item -> !isValidItem.test(item))) {
            return Mono.error(invalidRequestSupplier.get());
        }

        // Normalizar duplicados por productId con Streams + Collectors.toMap.
        Map<String, Integer> quantitiesByProduct = items.stream()
                .filter(isValidItem)
                .collect(Collectors.toMap(
                        OrderItemRequest::productId,
                        OrderItemRequest::quantity,
                        Integer::sum
                ));

        List<OrderItemRequest> normalizedItems = quantitiesByProduct.entrySet().stream()
                .map(e -> new OrderItemRequest(e.getKey(), e.getValue()))
                .toList();

        Supplier<String> orderIdSupplier = () -> UUID.randomUUID().toString();
        String orderId = orderIdSupplier.get();

        List<AllocateItemRequest> allocateItems = normalizedItems.stream()
                .map(i -> new AllocateItemRequest(i.productId(), i.quantity()))
                .toList();

        Consumer<Order> auditConsumer = order -> {
            log.debug("Pedido creado/confirmado: id={}, customerId={}", order.id(), order.customerId());
        };

        return inventoryClient.allocateInventory(orderId, allocateItems)
                .flatMap(remoteAllocation -> {
                    Order order = new Order(orderId, customerId, "CONFIRMED");

                    auditConsumer.accept(order);

                    return orderRepository.insert(order)
                            .then(Mono.defer(() -> {
                                List<OrderItem> orderItems = remoteAllocation.allocatedItems().stream()
                                        .map(allocated ->
                                                new OrderItem(
                                                        UUID.randomUUID().toString(),
                                                        order.id(),
                                                        allocated.productId(),
                                                        allocated.quantity()
                                                )
                                        )
                                        .toList();

                                return Flux.fromIterable(orderItems)
                                        .flatMap(orderItemRepository::insert)
                                        .then()
                                        .thenReturn(new CreateOrderResponse(
                                                order.id(),
                                                order.customerId(),
                                                order.status(),
                                                remoteAllocation.allocatedItems().stream()
                                                        .map(ai -> new AllocatedItemResponse(ai.productId(), ai.quantity()))
                                                        .toList()
                                        ));
                            }));
                });
    }

    @Override
    public Mono<GetOrderResponse> getOrderById(String orderId) {
        Supplier<OrderNotFoundException> notFoundSupplier = () ->
                new OrderNotFoundException("Pedido no encontrado: " + orderId, orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(notFoundSupplier.get()))
                .flatMap(order ->
                        orderItemRepository.findByOrderId(order.id())
                                .map(item -> new AllocatedItemResponse(item.productId(), item.quantity()))
                                .collectList()
                                .map(items -> new GetOrderResponse(order.id(), order.customerId(), order.status(), items))
                );
    }
}

