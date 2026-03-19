package com.challenge.orders.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entidad persistida en H2 vía R2DBC.
 */
@Table("orders")
public record Order(@Id String id, String customerId, String status) {
}

