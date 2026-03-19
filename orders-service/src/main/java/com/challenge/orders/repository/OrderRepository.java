package com.challenge.orders.repository;

import com.challenge.orders.domain.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, String> {

    @Query("""
            INSERT INTO orders (id, customer_id, status)
            VALUES (:#{#order.id}, :#{#order.customerId}, :#{#order.status})
            """)
    Mono<Order> insert(Order order);
}

