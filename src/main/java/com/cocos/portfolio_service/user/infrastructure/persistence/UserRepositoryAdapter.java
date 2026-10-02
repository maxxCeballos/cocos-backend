package com.cocos.portfolio_service.user.infrastructure.persistence;

import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository repository;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(UserJpaRepository repository, UserMapper userMapper) {
        this.repository = repository;
        this.userMapper = userMapper;
    }

    public Optional<User> findById(Long userId) {
        Optional<UserEntity> userEntity = repository.findById(userId);

        return userEntity.map(userMapper::toUserDomain);
    }

    @Override
    public boolean existsById(Long userId) {
        return repository.existsById(userId);
    }
}
