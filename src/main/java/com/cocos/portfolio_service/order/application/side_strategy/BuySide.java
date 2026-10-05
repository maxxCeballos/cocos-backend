package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.shared.domain.money.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service("BUY")
public class BuySide implements SideStrategy {

    public BuySide() {}

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        OrderStatus status = orderToSubmit.type().equals(OrderType.MARKET) ? OrderStatus.FILLED : OrderStatus.NEW;

        return new Order(
                null,
                context.user().id(),
                context.instrument().id(),
                OrderSide.BUY,
                orderToSubmit.size(),
                null,
                orderToSubmit.type(),
                status,
                LocalDateTime.now());
    }

    private void calculateMoneyToSpend(Long size) {
        if(size > 0) {
            // TODO: se toma en cuenta el size no el presupuesto
        } else {
            // TODO: se toma en cuenta el presupuesto
        }
    }

    private boolean hasEnoughMoney(Long cashOut, Money.ARS availableCash) {
        return BigDecimal.valueOf(cashOut).compareTo(availableCash.value()) < 1;
    }
}
