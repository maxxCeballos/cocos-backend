package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import org.springframework.stereotype.Service;

@Service("BUY")
public class BuySide implements SideStrategy {

    private final OrderRepository orderRepository;

    public BuySide(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        System.out.printf("ORDEN EXECUTADA POR BUY");

        return null;
    }
}
