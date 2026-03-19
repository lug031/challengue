package com.challenge.orders.error;

public class RemoteUnavailableException extends RuntimeException {
    public RemoteUnavailableException(String message) {
        super(message);
    }

    public RemoteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

