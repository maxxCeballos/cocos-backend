package com.cocos.portfolio_service.order.domain;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.shared.domain.money.Money;

public record OrderToSubmit(
        Long instrumentId,
        OrderSide side,
        OrderType type,
        Long size,
        Money.ARS price,
        Money.ARS budget) {
}
