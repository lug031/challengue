package com.challenge.orders.service;

import com.challenge.orders.dto.CreateOrderRequest;
import com.challenge.orders.dto.CreateOrderResponse;
import com.challenge.orders.dto.GetOrderResponse;
import reactor.core.publisher.Mono;

/**
 * Reglas de negocio del Servicio de Pedidos.
 */
public interface OrdersService {
    /**
     * Crea un pedido, lo confirma y solicita la asignación de inventario.
     */
    Mono<CreateOrderResponse> createOrder(CreateOrderRequest request);

    /**
     * Obtiene un pedido por id.
     */
    Mono<GetOrderResponse> getOrderById(String orderId);
}

