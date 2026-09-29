package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
class OrderService implements IOrderService {
    @Override
    public Order submit(OrderToSubmit command) {
        return new Order(
                90L,
                command.userId(),
                command.instrumentId(),
                command.side(),
                command.quantity(),
                command.price() != null ? command.price() : new BigDecimal("100.00"),
                command.type(),
                "FILLED",
                Instant.parse("2026-09-29T12:00:00Z"));
    }
}
