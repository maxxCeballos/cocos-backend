package com.cocos.portfolio_service.order.application.ports;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;

import java.util.List;

public interface OrderRepository {
    List<Order> findEffectiveOrdersByUserId(Long userId);
    Order save(Order order);
}
