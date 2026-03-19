package com.challenge.inventory.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entidad persistida en H2 vía R2DBC.
 * Es inmutable para favorecer un estilo funcional.
 */
@Table("products")
public record Product(@Id String id, String name, int stock) {
}

