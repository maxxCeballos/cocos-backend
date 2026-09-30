package com.cocos.portfolio_service.order.infrastructure.persistence;

import com.cocos.portfolio_service.order.application.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository repository;

    public OrderRepositoryAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return repository.findByUserIdAndStatus(Math.toIntExact(userId), status).stream().map(OrderEntity::toDomain).toList();
    }

    @Override
    public Order save(Order order) {
        return repository.save(OrderEntity.fromDomain(order)).toDomain();
    }
}
