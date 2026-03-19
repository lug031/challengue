package com.challenge.orders.controller;

import com.challenge.orders.dto.CreateOrderRequest;
import com.challenge.orders.dto.CreateOrderResponse;
import com.challenge.orders.dto.GetOrderResponse;
import com.challenge.orders.service.OrdersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

/**
 * Endpoints del Servicio de Pedidos.
 */
@RestController
@RequestMapping(path = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    /**
     * Crea un pedido y confirma su estado usando la asignación de inventario.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateOrderResponse> createOrder(@RequestBody Mono<CreateOrderRequest> request) {
        return request.flatMap(ordersService::createOrder);
    }

    /**
     * Obtiene un pedido por id.
     */
    @GetMapping(path = "/{orderId}")
    public Mono<GetOrderResponse> getOrder(@PathVariable String orderId) {
        return ordersService.getOrderById(orderId);
    }
}

