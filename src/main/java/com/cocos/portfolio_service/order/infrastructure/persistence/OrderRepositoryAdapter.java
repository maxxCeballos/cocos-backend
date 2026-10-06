package com.cocos.portfolio_service.order.infrastructure.persistence;

import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.utils.mappers.OrderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository repository;
    private final OrderMapper orderMapper;

    public OrderRepositoryAdapter(OrderJpaRepository repository, OrderMapper orderMapper) {
        this.repository = repository;
        this.orderMapper = orderMapper;
    }

    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status){
        List<OrderEntity> ordersDB = repository.findByUserIdAndStatus(userId, status);
        return ordersDB.stream().map(orderMapper::toOrderDomain).toList();
    }


    @Override
    public List<Order> findEffectiveOrdersByUserId(Long userId) {
        List<OrderEntity> ordersDB = repository.findByUserIdAndStatusNotIn(userId, List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED));
        return ordersDB.stream().map(orderMapper::toOrderDomain).toList();
    }

    @Override
    public Order save(Order order) {
        return orderMapper.toOrderDomain(repository.save(orderMapper.toOrderEntity(order)));
    }
}
