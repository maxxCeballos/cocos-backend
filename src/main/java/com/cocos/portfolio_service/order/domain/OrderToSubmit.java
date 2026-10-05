package com.cocos.portfolio_service.order.domain;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;

import java.math.BigDecimal;

public record OrderToSubmit(
        Long instrumentId,
        OrderSide side,
        OrderType type,
        Long size,
        BigDecimal budget) {
}
