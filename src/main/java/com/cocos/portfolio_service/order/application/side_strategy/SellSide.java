package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
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
import java.util.Optional;

@Log4j2
@Service("SELL")
public class SellSide implements SideStrategy {

    private final MarketDataRepository marketDataRepository;

    public SellSide(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        Long instrumentId = context.instrument().id();
        OrderStatus status = orderToSubmit.type().equals(OrderType.MARKET) ? OrderStatus.FILLED : OrderStatus.NEW;
        Long size = orderToSubmit.size();

        Optional<MarketData> marketDataOpt = marketDataRepository.findLatestByInstrumentId(instrumentId);
        if(marketDataOpt.isEmpty()) throw new MarketDataNotFoundException(instrumentId);

        Money.ARS moneyToSellByUnit = new Money.ARS(marketDataOpt.get().close());
        if(OrderType.LIMIT.equals(orderToSubmit.type())) {
            moneyToSellByUnit = orderToSubmit.price();
        }

        Long cantInstruments = calculateInstrumentPossessions(context.orders(), instrumentId);
        if(size > cantInstruments) {
            log.error("la cantidad a vender excede la cantidad disponible userId: {} instrumentId: {}", context.user().id(), instrumentId);
            status = OrderStatus.REJECTED;
        }


        return new Order(
                null,
                context.user().id(),
                instrumentId,
                OrderSide.SELL,
                size,
                moneyToSellByUnit.value(),
                orderToSubmit.type(),
                status,
                LocalDateTime.now());
    }

    Long calculateInstrumentPossessions(List<Order> orders, Long instrumentId) {
        long possessions = 0;
        for (Order order : orders) {
            if (!instrumentId.equals(order.instrumentId())) continue;
            if (order.toShareSwapped()) {
                possessions += order.size();
            } else if (order.toCashSwapped() || order.isShareOnHold()) {
                possessions -= order.size();
            }
        }
        return possessions;
    }
}
