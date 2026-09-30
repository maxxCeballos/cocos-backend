package com.cocos.portfolio_service.user.infrastructure.persistence;

import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository repository;

    public UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsById(Long userId) {
        return repository.existsById(userId);
    }
}
