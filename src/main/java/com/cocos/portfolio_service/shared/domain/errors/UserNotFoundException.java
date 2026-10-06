package com.cocos.portfolio_service.shared.domain.errors;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User with id " + userId + " was not found");
    }
}
