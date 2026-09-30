package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;

import java.util.List;

public interface OrderRepository {
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    Order save(Order order);
}
