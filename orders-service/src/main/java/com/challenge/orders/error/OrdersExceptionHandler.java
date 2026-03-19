package com.challenge.orders.error;

import com.challenge.common.api.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Traduce excepciones del Servicio de Pedidos a respuestas {@link ApiError}.
 */
@RestControllerAdvice
public class OrdersExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrdersExceptionHandler.class);

    @ExceptionHandler(OrderInvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(OrderInvalidRequestException ex) {
        log.debug("Request inválido para pedidos: {}", ex.getMessage());
        ApiError payload = new ApiError("ORDER_INVALID", ex.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
    }

    @ExceptionHandler(RemoteProductNotFoundException.class)
    public ResponseEntity<ApiError> handleRemoteProductNotFound(RemoteProductNotFoundException ex) {
        log.debug("Producto no encontrado en Inventario. productId={}", ex.getProductId());
        ApiError payload = new ApiError(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                List.of(ex.getProductId())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(payload);
    }

    @ExceptionHandler(RemoteInsufficientStockException.class)
    public ResponseEntity<ApiError> handleRemoteInsufficientStock(RemoteInsufficientStockException ex) {
        log.debug(
                "Stock insuficiente en Inventario. productId={}, requested={}, available={}",
                ex.getProductId(),
                ex.getRequestedQuantity(),
                ex.getAvailableStock()
        );
        ApiError payload = new ApiError(
                "INSUFFICIENT_STOCK",
                ex.getMessage(),
                List.of(
                        ex.getProductId(),
                        String.valueOf(ex.getRequestedQuantity()),
                        String.valueOf(ex.getAvailableStock())
                )
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(payload);
    }

    @ExceptionHandler(RemoteUnavailableException.class)
    public ResponseEntity<ApiError> handleRemoteUnavailable(RemoteUnavailableException ex) {
        log.warn("Inventario remoto no disponible: {}", ex.getMessage());
        ApiError payload = new ApiError("REMOTE_UNAVAILABLE", ex.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(payload);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderNotFound(OrderNotFoundException ex) {
        log.debug("Pedido no encontrado. orderId={}", ex.getOrderId());
        ApiError payload = new ApiError("ORDER_NOT_FOUND", ex.getMessage(), List.of(ex.getOrderId()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(payload);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Error inesperado en Pedidos", ex);
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = ex.getMessage() == null ? "sin mensaje" : ex.getMessage();
        String rootMessage = root.getMessage() == null ? "sin mensaje (root)" : root.getMessage();

        ApiError payload = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "Error interno del servidor",
                List.of(ex.getClass().getSimpleName(), message, rootMessage)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
    }
}

