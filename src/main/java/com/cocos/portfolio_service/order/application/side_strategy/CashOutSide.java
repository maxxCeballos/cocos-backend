package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import com.cocos.portfolio_service.shared.domain.money.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service("CASH_OUT")
public class CashOutSide implements SideStrategy {

    private static final BigDecimal CASH_OUT_PRICE = BigDecimal.ONE;

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        OrderStatus status = OrderStatus.FILLED;

        if (orderToSubmit.type() != OrderType.MARKET) {
            throw new InvalidOrderException("Cash-out orders must use MARKET type");
        }
        if (!InstrumentType.MONEDA.equals(context.instrument().type())) {
            throw new InvalidOrderException("Cash-out orders require a MONEDA instrument");
        }
        if(!hasEnoughMoney(orderToSubmit.size(), context.availableCash())) {
            status = OrderStatus.REJECTED;
        }

        return new Order(
                null,
                context.user().id(),
                context.instrument().id(),
                OrderSide.CASH_OUT,
                orderToSubmit.size(),
                CASH_OUT_PRICE,
                OrderType.MARKET,
                status,
                LocalDateTime.now());
    }

    private boolean hasEnoughMoney(Long cashOut, Money.ARS availableCash) {
        return BigDecimal.valueOf(cashOut).compareTo(availableCash.value()) < 1;
    }
}
