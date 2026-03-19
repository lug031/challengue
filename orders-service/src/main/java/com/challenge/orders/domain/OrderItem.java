package com.challenge.orders.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Item de un pedido persistido en H2 vía R2DBC.
 */
@Table("order_items")
public record OrderItem(@Id String id, String orderId, String productId, int quantity) {
}

