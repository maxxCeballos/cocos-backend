package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.shared.domain.money.Money;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Log4j2
@Service("BUY")
public class BuySide implements SideStrategy {

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        Long instrumentId = orderToSubmit.instrumentId();
        OrderStatus status = orderToSubmit.type().equals(OrderType.MARKET) ? OrderStatus.FILLED : OrderStatus.NEW;
        Long size = orderToSubmit.size();

        Money.ARS moneyToInvestByUnit = new Money.ARS(context.marketData().close());
        if(OrderType.LIMIT.equals(orderToSubmit.type())) {
            moneyToInvestByUnit = orderToSubmit.price();
        }

        // SIZE tiene prioridad por sobre budget
        if(size > 0) {
            if(!hasEnoughMoneyBySize(context.availableCash(), orderToSubmit.size(), moneyToInvestByUnit)) {
                log.error("no hay suficiente dinero disponible de userId: {} para la compra del instrumento: {}", context.user().id(), instrumentId);
                status = OrderStatus.REJECTED;
            }
        } else {
            size = buyByBudget(context.availableCash(), orderToSubmit.budget(), moneyToInvestByUnit);
            if(size == -1) {
                status = OrderStatus.REJECTED;
            }
        }

        return new Order(
                null,
                context.user().id(),
                context.instrument().id(),
                OrderSide.BUY,
                size,
                moneyToInvestByUnit.value(),
                orderToSubmit.type(),
                status,
                LocalDateTime.now());
    }

    private boolean hasEnoughMoneyBySize(Money.ARS availableCash, Long size, Money.ARS close) {
        Money.ARS totalPrice = close.multiply(BigDecimal.valueOf(size));
        return hasEnoughMoney(availableCash, totalPrice);
    }

    private Long buyByBudget(Money.ARS availableCash, Money.ARS budget, Money.ARS close) {
        Long size;

        if(!hasEnoughMoney(availableCash, budget)) {
            size = -1L;
        } else {
            size = budget.value().divide(close.value(), 0, RoundingMode.DOWN).longValue();
        }

        return size;
    }

    private boolean hasEnoughMoney(Money.ARS availableCash, Money.ARS cashOut) {
        return cashOut.value().compareTo(availableCash.value()) < 1;
    }
}
