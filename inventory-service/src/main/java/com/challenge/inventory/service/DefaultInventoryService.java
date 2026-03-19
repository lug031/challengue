package com.challenge.inventory.service;

import com.challenge.inventory.domain.Product;
import com.challenge.inventory.error.InsufficientStockException;
import com.challenge.inventory.error.InventoryInvalidRequestException;
import com.challenge.inventory.error.ProductNotFoundException;
import com.challenge.inventory.repository.ProductRepository;
import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import com.challenge.common.contracts.inventory.AllocatedItemResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementación reactiva del Servicio de Inventario.
 */
@Service
public class DefaultInventoryService implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(DefaultInventoryService.class);
    private final ProductRepository productRepository;

    public DefaultInventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Mono<AllocateInventoryResponse> allocateInventory(AllocateInventoryRequest request) {
        Supplier<InventoryInvalidRequestException> invalidRequestSupplier = () ->
                new InventoryInvalidRequestException("El request de asignacion de inventario es invalido");

        Predicate<String> isNonBlank = value ->
                Optional.ofNullable(value).map(String::trim).filter(v -> !v.isBlank()).isPresent();

        Predicate<AllocateItemRequest> isValidItem = item ->
                item != null
                        && isNonBlank.test(item.productId())
                        && item.quantity() > 0;

        Optional<String> orderIdOpt = Optional.ofNullable(request)
                .map(AllocateInventoryRequest::orderId)
                .map(s -> s == null ? null : s.trim())
                .filter(isNonBlank);

        if (orderIdOpt.isEmpty()) {
            return Mono.error(invalidRequestSupplier.get());
        }

        String orderId = orderIdOpt.get();

        List<AllocateItemRequest> items = Optional.ofNullable(request.items()).orElse(List.of());
        if (items.isEmpty() || items.stream().anyMatch(item -> !isValidItem.test(item))) {
            return Mono.error(invalidRequestSupplier.get());
        }

        Map<String, Integer> quantitiesByProduct = items.stream()
                .filter(isValidItem)
                .collect(Collectors.toMap(
                        AllocateItemRequest::productId,
                        AllocateItemRequest::quantity,
                        Integer::sum
                ));

        Consumer<Product> auditConsumer = product -> {
            log.debug("Inventario actualizado: id={}, stock={}", product.id(), product.stock());
        };

        List<AllocateItemRequest> normalizedItems = quantitiesByProduct.entrySet().stream()
                .map(e -> new AllocateItemRequest(e.getKey(), e.getValue()))
                .toList();

        Flux<AllocatedItemResponse> allocationFlux = Flux.fromIterable(normalizedItems)
                .concatMap(item -> allocateSingle(item, auditConsumer));

        return allocationFlux.collectList()
                .map(allocatedItems -> new AllocateInventoryResponse(orderId, allocatedItems));
    }

    private Mono<AllocatedItemResponse> allocateSingle(AllocateItemRequest item, Consumer<Product> auditConsumer) {
        Supplier<ProductNotFoundException> notFoundSupplier =
                () -> new ProductNotFoundException("Producto no encontrado: " + item.productId(), item.productId());

        return productRepository.findById(item.productId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(productOpt -> productOpt
                        .map(product -> {
                            Supplier<InsufficientStockException> insufficientSupplier = () ->
                                    new InsufficientStockException(
                                            "Stock insuficiente para producto: " + item.productId(),
                                            product.id(),
                                            product.stock(),
                                            item.quantity()
                                    );

                            if (product.stock() < item.quantity()) {
                                return Mono.<AllocatedItemResponse>error(insufficientSupplier.get());
                            }

                            int newStock = product.stock() - item.quantity();
                            Product updated = new Product(product.id(), product.name(), newStock);

                            return productRepository.save(updated)
                                    .doOnNext(auditConsumer)
                                    .map(saved -> new AllocatedItemResponse(saved.id(), item.quantity()));
                        })
                        .orElseGet(() -> Mono.error(notFoundSupplier.get()))
                );
    }
}

