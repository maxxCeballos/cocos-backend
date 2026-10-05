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

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service("SELL")
public class SellSide implements SideStrategy {

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        OrderStatus status = orderToSubmit.type().equals(OrderType.MARKET) ? OrderStatus.FILLED : OrderStatus.NEW;
        Long size = orderToSubmit.size();

        Money.ARS moneyToSellByUnit = new Money.ARS(context.marketData().close());
        if(OrderType.LIMIT.equals(orderToSubmit.type())) {
            moneyToSellByUnit = orderToSubmit.price();
        }

        Long cantInstruments = calculateInstrumentPossessions(context.orders());
        if(size > cantInstruments) {
            log.error("la cantidad a vender excede la cantidad disponible userId: {} instrumentId: {}", context.user().id(), context.instrument().id());
            status = OrderStatus.REJECTED;
        }


        return new Order(
                null,
                context.user().id(),
                context.instrument().id(),
                OrderSide.SELL,
                size,
                moneyToSellByUnit.value(),
                orderToSubmit.type(),
                status,
                LocalDateTime.now());
    }

    Long calculateInstrumentPossessions(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.toShareSwapped() || order.toCashSwapped() || order.isShareOnHold())
                .mapToLong(order -> order.toShareSwapped() ? order.size() : -order.size())
                .sum();
    }
}
