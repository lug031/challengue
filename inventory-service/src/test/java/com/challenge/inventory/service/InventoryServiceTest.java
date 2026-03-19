package com.challenge.inventory.service;

import com.challenge.inventory.domain.Product;
import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import com.challenge.common.contracts.inventory.AllocatedItemResponse;
import com.challenge.inventory.error.InsufficientStockException;
import com.challenge.inventory.error.InventoryInvalidRequestException;
import com.challenge.inventory.error.ProductNotFoundException;
import com.challenge.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del Servicio de Inventario (validaciones, ausencia y stock insuficiente).
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    DefaultInventoryService inventoryService;

    @Test
    void allocateInventory_success_whenStockSufficient() {
        String productId = "11111111-1111-1111-1111-111111111111";
        Product product = new Product(productId, "Producto A", 10);
        Product updated = new Product(productId, "Producto A", 7);

        when(productRepository.findById(eq(productId))).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(updated));

        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "order-1",
                List.of(new AllocateItemRequest(productId, 3))
        );

        Mono<com.challenge.common.contracts.inventory.AllocateInventoryResponse> result = inventoryService.allocateInventory(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.orderId()).isEqualTo("order-1");
                    assertThat(response.allocatedItems()).hasSize(1);
                    AllocatedItemResponse item = response.allocatedItems().get(0);
                    assertThat(item.productId()).isEqualTo(productId);
                    assertThat(item.quantity()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    void allocateInventory_error_whenProductMissing() {
        String productId = "33333333-3333-3333-3333-333333333333";
        when(productRepository.findById(eq(productId))).thenReturn(Mono.empty());

        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "order-1",
                List.of(new AllocateItemRequest(productId, 1))
        );

        StepVerifier.create(inventoryService.allocateInventory(request))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ProductNotFoundException.class);
                    ProductNotFoundException ex = (ProductNotFoundException) throwable;
                    assertThat(ex.getProductId()).isEqualTo(productId);
                })
                .verify();

        verify(productRepository, times(1)).findById(eq(productId));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void allocateInventory_error_whenStockInsufficient() {
        String productId = "11111111-1111-1111-1111-111111111111";
        Product product = new Product(productId, "Producto A", 2);
        when(productRepository.findById(eq(productId))).thenReturn(Mono.just(product));

        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "order-1",
                List.of(new AllocateItemRequest(productId, 5))
        );

        StepVerifier.create(inventoryService.allocateInventory(request))
                .expectErrorSatisfies(throwable -> assertThat(throwable).isInstanceOf(InsufficientStockException.class))
                .verify();

        verify(productRepository, times(1)).findById(eq(productId));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void allocateInventory_error_whenRequestInvalid() {
        AllocateInventoryRequest request = new AllocateInventoryRequest(
                "   ",
                List.of(new AllocateItemRequest("11111111-1111-1111-1111-111111111111", 0))
        );

        StepVerifier.create(inventoryService.allocateInventory(request))
                .expectErrorSatisfies(throwable -> assertThat(throwable).isInstanceOf(InventoryInvalidRequestException.class))
                .verify();

        verifyNoInteractions(productRepository);
    }
}

