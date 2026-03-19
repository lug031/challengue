package com.challenge.orders.repository;

import com.challenge.orders.domain.OrderItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItem, String> {

    @Query("""
            INSERT INTO order_items (id, order_id, product_id, quantity)
            VALUES (:#{#item.id}, :#{#item.orderId}, :#{#item.productId}, :#{#item.quantity})
            """)
    Mono<OrderItem> insert(OrderItem item);

    @Query("""
            SELECT id, order_id, product_id, quantity
            FROM order_items
            WHERE order_id = :orderId
            """)
    Flux<OrderItem> findByOrderId(String orderId);
}

