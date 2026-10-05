package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service("CASH_IN")
public class CashInSide implements SideStrategy {

    private static final BigDecimal CASH_IN_PRICE = BigDecimal.ONE;

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        if (orderToSubmit.type() != OrderType.MARKET) {
            throw new InvalidOrderException("Cash-in orders must use MARKET type");
        }
        if (orderToSubmit.price().compareTo(CASH_IN_PRICE) != 0) {
            throw new InvalidOrderException("Cash-in order price must be 1");
        }
        if (!InstrumentType.MONEDA.equals(context.instrument().type())) {
            throw new InvalidOrderException("Cash-in orders require a MONEDA instrument");
        }

        return new Order(
                null,
                context.user().id(),
                context.instrument().id(),
                OrderSide.CASH_IN,
                orderToSubmit.size(),
                CASH_IN_PRICE,
                OrderType.MARKET,
                OrderStatus.FILLED,
                LocalDateTime.now());
    }
}
