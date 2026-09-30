package com.cocos.portfolio_service.order.infrastructure.persistence;

import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdAndStatus(Long userId, OrderStatus status);
}
