package com.challenge.common.api;

import java.util.List;

/**
 * DTO de error para respuestas REST.
 */
public record ApiError(String code, String message, List<String> details) {
}

