package com.cocos.portfolio_service.shared.domain.errors;

public class ValkeyUnavailableException extends RuntimeException {
    public ValkeyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValkeyUnavailableException(String message) {
        super(message);
    }
}
