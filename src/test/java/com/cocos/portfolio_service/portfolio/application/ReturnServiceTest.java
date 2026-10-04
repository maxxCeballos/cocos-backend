package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReturnServiceTest {
    private final ReturnService returnService = new ReturnService();

    @Test
    void dailyPositionReturn_matchesInstrument54WhenPurchaseIsInsideMarketWindow() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData("229.50", "232.00", 14), marketData("232.00", "229.00", 13)),
                List.of(order(OrderSide.BUY, 500, "250.00", OrderStatus.FILLED, 13, 14)));

        assertEquals(new BigDecimal("-8.2000"), result);
    }

    @Test
    void dailyPositionReturn_includesPurchaseCostForOlderHoldings() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData("229.50", "232.00", 14), marketData("232.00", "229.00", 13)),
                List.of(order(OrderSide.BUY, 500, "250.00", OrderStatus.FILLED, 12, 14)));

        assertEquals(new BigDecimal("-8.2000"), result);
    }

    @Test
    void dailyPositionReturn_sumsDailyPnlForInstrument47() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData(47L, "925.85", "921.80", 14),
                        marketData(47L, "921.80", "893.65", 13)),
                List.of(
                        order(47L, OrderSide.BUY, 50, "930.00", OrderStatus.FILLED, 12, 10),
                        order(47L, OrderSide.SELL, 10, "940.00", OrderStatus.FILLED, 12, 11)));

        assertEquals(new BigDecimal("-0.1419"), result);
    }

    @Test
    void dailyPositionReturn_sumsDailyPriceChangesAroundBuysAndSells() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData("105.00", "110.00", 15), marketData("110.00", "100.00", 14),
                        marketData("100.00", "90.00", 13)),
                List.of(
                        order(OrderSide.BUY, 10, "100.00", OrderStatus.FILLED, 13, 10),
                        order(OrderSide.SELL, 5, "100.00", OrderStatus.FILLED, 15, 10)));

        assertEquals(new BigDecimal("2.5000"), result);
    }

    @Test
    void dailyPositionReturn_valuesBuyAfterLatestMarketDateAtLatestClose() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData("110.00", "100.00", 13)),
                List.of(order(OrderSide.BUY, 10, "100.00", OrderStatus.FILLED, 14, 10)));

        assertEquals(new BigDecimal("10.0000"), result);
    }

    @Test
    void dailyPositionReturn_assignsTradesBetweenMarketDatesToNextQuote() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData("110.00", "100.00", 15), marketData("100.00", "90.00", 13)),
                List.of(order(OrderSide.BUY, 10, "105.00", OrderStatus.FILLED, 14, 10)));

        assertEquals(new BigDecimal("4.7619"), result);
    }

    @Test
    void dailyPositionReturn_ignoresUnfilledOrdersAndReturnsZeroWithoutOpenExposure() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(marketData("110.00", "100.00", 14)),
                List.of(order(OrderSide.BUY, 10, "100.00", OrderStatus.NEW, 13, 10)));

        assertEquals(new BigDecimal("0.0000"), result);
    }

    @Test
    void dailyPositionReturn_returnsZeroWithoutValidMarketData() {
        var result = returnService.calculateDailyPositionReturn(
                List.of(), List.of(order(OrderSide.BUY, 10, "100.00", OrderStatus.FILLED, 13, 10)));

        assertEquals(new BigDecimal("0.0000"), result);
    }

    private MarketData marketData(String close, String previousClose, int day) {
        return marketData(54L, close, previousClose, day);
    }

    private MarketData marketData(Long instrumentId, String close, String previousClose, int day) {
        return new MarketData((long) day, instrumentId, new BigDecimal(close), new BigDecimal(previousClose),
                LocalDate.of(2023, 7, day));
    }

    private Order order(OrderSide side, long size, String price, OrderStatus status, int day, int hour) {
        return order(54L, side, size, price, status, day, hour);
    }

    private Order order(Long instrumentId, OrderSide side, long size, String price, OrderStatus status,
                        int day, int hour) {
        return new Order(1L, 1L, instrumentId, side, size, new BigDecimal(price), OrderType.MARKET, status,
                LocalDateTime.of(2023, 7, day, hour, 0));
    }
}
