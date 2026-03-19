package com.challenge.orders.client;

import com.challenge.common.api.ApiError;
import com.challenge.common.contracts.inventory.AllocateInventoryRequest;
import com.challenge.common.contracts.inventory.AllocateInventoryResponse;
import com.challenge.common.contracts.inventory.AllocateItemRequest;
import com.challenge.orders.error.RemoteInsufficientStockException;
import com.challenge.orders.error.RemoteProductNotFoundException;
import com.challenge.orders.error.RemoteUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link InventoryClient} usando {@link WebClient}.
 */
@Component
public class WebClientInventoryClient implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientInventoryClient.class);

    private final WebClient webClient;

    public WebClientInventoryClient(@Value("${orders.inventory.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Llama al Servicio de Inventario para asignar stock a los ítems del pedido.
     */
    @Override
    public Mono<AllocateInventoryResponse> allocateInventory(String orderId, List<AllocateItemRequest> items) {
        log.debug(
                "Solicitando asignación de inventario. orderId={}, itemsCount={}",
                orderId,
                items == null ? 0 : items.size()
        );
        AllocateInventoryRequest request = new AllocateInventoryRequest(orderId, items);

        return webClient.post()
                .uri("/inventory/allocate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == 404, response ->
                        response.bodyToMono(ApiError.class)
                                .map(apiError -> {
                                    String productId = firstDetail(apiError.details(), 0).orElse("unknown-product");
                                    log.debug("Inventario 404 para orderId={}, productId={}", orderId, productId);
                                    return new RemoteProductNotFoundException(apiError.message(), productId);
                                })
                )
                .onStatus(status -> status.value() == 409, response ->
                        response.bodyToMono(ApiError.class)
                                .map(apiError -> {
                                    String productId = firstDetail(apiError.details(), 0).orElse("unknown-product");
                                    int requested = parseIntDetail(apiError.details(), 1).orElse(0);
                                    int available = parseIntDetail(apiError.details(), 2).orElse(0);
                                    log.debug(
                                            "Inventario 409 para orderId={}, productId={}, requested={}, available={}",
                                            orderId,
                                            productId,
                                            requested,
                                            available
                                    );
                                    return new RemoteInsufficientStockException(apiError.message(), productId, requested, available);
                                })
                )
                .onStatus(status -> status.value() >= 500 && status.value() <= 599, response ->
                        {
                            int statusCode = response.statusCode().value();
                            return response.bodyToMono(ApiError.class)
                                    .defaultIfEmpty(new ApiError("REMOTE_UNAVAILABLE", "Inventario no disponible", List.of()))
                                    .map(apiError -> {
                                        log.warn(
                                                "Inventario 5xx para orderId={}, statusCode={}, message={}",
                                                orderId,
                                                statusCode,
                                                apiError.message()
                                        );
                                        return new RemoteUnavailableException(apiError.message());
                                    });
                        }
                )
                .bodyToMono(AllocateInventoryResponse.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(throwable -> {
                    if (throwable instanceof RemoteProductNotFoundException
                            || throwable instanceof RemoteInsufficientStockException) {
                        return Mono.error(throwable);
                    }
                    log.warn("No se pudo contactar Inventario. orderId={}", orderId);
                    log.debug("Detalle al contactar Inventario. orderId={}", orderId, throwable);
                    return Mono.error(new RemoteUnavailableException("No se pudo contactar Inventario", throwable));
                });
    }

    private static Optional<String> firstDetail(List<String> details, int index) {
        return Optional.ofNullable(details)
                .flatMap(list -> list.size() > index ? Optional.ofNullable(list.get(index)) : Optional.empty());
    }

    private static Optional<Integer> parseIntDetail(List<String> details, int index) {
        return firstDetail(details, index).flatMap(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        });
    }

}

