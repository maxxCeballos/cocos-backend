package com.cocos.portfolio_service.order.domain;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Order(
        Long id,
        Long userId,
        Long instrumentId,
        OrderSide side,
        Long size,
        BigDecimal price,
        OrderType type,
        OrderStatus status,
        LocalDateTime datetime) {

    public boolean isCashOnHold() {
        return this.side().equals(OrderSide.BUY) && this.status().equals(OrderStatus.NEW);
    }

    public boolean isShareOnHold() {
        return this.side().equals(OrderSide.SELL) && this.status().equals(OrderStatus.NEW);
    }

    public boolean swapToCash() {
        return this.side().equals(OrderSide.SELL) && this.status().equals(OrderStatus.FILLED);
    }
}
