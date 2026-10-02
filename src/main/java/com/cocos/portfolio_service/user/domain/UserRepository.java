package com.cocos.portfolio_service.user.domain;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);
    boolean existsById(Long userId);
}
