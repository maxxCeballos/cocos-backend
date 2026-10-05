package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellSideTest {
    private final SellSide sellSide = new SellSide();

    @Test
    void calculateInstrumentPossessions_sumsFilledBuysAndSubtractsFilledAndNewSells() {
        var orders = List.of(
                order(OrderSide.BUY, 10L, OrderStatus.FILLED),
                order(OrderSide.BUY, 5L, OrderStatus.FILLED),
                order(OrderSide.SELL, 3L, OrderStatus.FILLED),
                order(OrderSide.SELL, 4L, OrderStatus.NEW));

        assertEquals(8L, sellSide.calculateInstrumentPossessions(orders));
    }

    @Test
    void calculateInstrumentPossessions_returnsNegativeResultWhenSalesExceedPurchases() {
        var orders = List.of(
                order(OrderSide.SELL, 3L, OrderStatus.FILLED),
                order(OrderSide.SELL, 2L, OrderStatus.NEW));

        assertEquals(-5L, sellSide.calculateInstrumentPossessions(orders));
    }

    private Order order(OrderSide side, long size, OrderStatus status) {
        return new Order(1L, 7L, 3L, side, size, BigDecimal.ONE, OrderType.MARKET,
                status, LocalDateTime.of(2023, 7, 13, 12, 0));
    }
}
