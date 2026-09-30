package com.cocos.portfolio_service.user.domain;

public interface UserRepository {
    boolean existsById(Long userId);
}
