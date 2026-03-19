package com.challenge.inventory.error;

import com.challenge.common.api.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Traduce excepciones del Servicio de Inventario a respuestas {@link ApiError}.
 */
@RestControllerAdvice
public class InventoryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException ex) {
        log.debug("Producto no encontrado. productId={}", ex.getProductId());
        ApiError payload = new ApiError(
                "PRODUCT_NOT_FOUND",
                ex.getMessage(),
                List.of(ex.getProductId())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(payload);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex) {
        log.debug(
                "Stock insuficiente. productId={}, requested={}, available={}",
                ex.getProductId(),
                ex.getRequestedQuantity(),
                ex.getAvailableStock()
        );
        ApiError payload = new ApiError(
                "INSUFFICIENT_STOCK",
                ex.getMessage(),
                List.of(ex.getProductId(), String.valueOf(ex.getRequestedQuantity()), String.valueOf(ex.getAvailableStock()))
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(payload);
    }

    @ExceptionHandler(InventoryInvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(InventoryInvalidRequestException ex) {
        log.debug("Request inválido para inventario: {}", ex.getMessage());
        ApiError payload = new ApiError(
                "INVENTORY_INVALID_REQUEST",
                ex.getMessage(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Error inesperado en Inventario", ex);
        String message = ex.getMessage() == null ? "sin mensaje" : ex.getMessage();
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String rootMessage = root.getMessage() == null ? "sin mensaje (root)" : root.getMessage();
        ApiError payload = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "Error interno del servidor",
                List.of(ex.getClass().getSimpleName(), message, rootMessage)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
    }
}

