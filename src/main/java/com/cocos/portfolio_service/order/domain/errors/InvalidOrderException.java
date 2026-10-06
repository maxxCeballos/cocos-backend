package com.cocos.portfolio_service.order.domain.errors;

public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
