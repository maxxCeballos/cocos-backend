package com.cocos.portfolio_service.order.domain;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(
        Long id,
        Long userId,
        Long instrumentId,
        OrderSide side,
        Long quantity,
        BigDecimal price,
        OrderType type,
        String status,
        Instant datetime) {
}
