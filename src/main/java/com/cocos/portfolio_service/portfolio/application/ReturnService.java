package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
class ReturnService {

    BigDecimal calculateDailyPositionReturn(List<MarketData> instrumentMarketData, List<Order> instrumentOrders) {
        if (instrumentMarketData == null || instrumentMarketData.isEmpty()
                || instrumentOrders == null || instrumentOrders.isEmpty()) {
            return BigDecimal.ZERO.setScale(4);
        }

        List<MarketData> orderedMarketData = instrumentMarketData.stream()
                .filter(data -> data != null && data.date() != null && data.close() != null
                        && data.previousClose() != null && data.previousClose().signum() > 0)
                .sorted(Comparator.comparing(MarketData::date))
                .toList();
        if (orderedMarketData.isEmpty()) return BigDecimal.ZERO.setScale(4);

        MarketData firstMarketData = orderedMarketData.getFirst();
        NavigableMap<LocalDate, Integer> marketDataIndexes = new TreeMap<>();
        for (int i = 0; i < orderedMarketData.size(); i++) {
            marketDataIndexes.putIfAbsent(orderedMarketData.get(i).date(), i);
        }

        Map<Integer, List<Order>> ordersByMarketDataIndex = new HashMap<>();
        Map<Integer, Long> sizeChangesByMarketDataIndex = new HashMap<>();
        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal profitAndLoss = BigDecimal.ZERO;
        long positionSize = 0;

        for (Order order : instrumentOrders) {
            if (order == null || order.status() != OrderStatus.FILLED
                    || (order.side() != OrderSide.BUY && order.side() != OrderSide.SELL)
                    || order.datetime() == null || order.price() == null || order.price().signum() <= 0
                    || order.size() == null || order.size() <= 0) {
                continue;
            }

            boolean isBuy = order.side() == OrderSide.BUY;
            if (isBuy) totalBuyAmount = totalBuyAmount.add(order.orderValue());

            LocalDate orderDate = order.datetime().toLocalDate();
            if (orderDate.isBefore(firstMarketData.date())) {
                positionSize += isBuy ? order.size() : -order.size();
                profitAndLoss = profitAndLoss.add(isBuy
                        ? order.orderValue().negate()
                        : order.orderValue());
                continue;
            }

            Map.Entry<LocalDate, Integer> matchingDate = marketDataIndexes.ceilingEntry(orderDate);
            int marketDataIndex = matchingDate == null
                    ? orderedMarketData.size() - 1
                    : matchingDate.getValue();
            ordersByMarketDataIndex.computeIfAbsent(marketDataIndex, ignored -> new ArrayList<>()).add(order);
            sizeChangesByMarketDataIndex.merge(marketDataIndex,
                    isBuy ? order.size() : -order.size(), Long::sum);
        }

        if (totalBuyAmount.signum() == 0) return BigDecimal.ZERO.setScale(4);

        profitAndLoss = profitAndLoss.add(
                firstMarketData.previousClose().multiply(BigDecimal.valueOf(positionSize)));
        for (int i = 0; i < orderedMarketData.size(); i++) {
            MarketData dailyMarketData = orderedMarketData.get(i);
            profitAndLoss = profitAndLoss.add(calculateDailyProfitAndLoss(
                    dailyMarketData, positionSize, ordersByMarketDataIndex.getOrDefault(i, List.of())));
            positionSize += sizeChangesByMarketDataIndex.getOrDefault(i, 0L);
        }

        return profitAndLoss.divide(totalBuyAmount, MathContext.DECIMAL128)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal calculateDailyProfitAndLoss(MarketData marketData, long positionSize,
                                                          List<Order> dailyOrders) {
        BigDecimal profitAndLoss = marketData.close().subtract(marketData.previousClose())
                .multiply(BigDecimal.valueOf(positionSize));

        for (Order order : dailyOrders) {
            BigDecimal tradePriceChange = order.side() == OrderSide.BUY
                    ? marketData.close().subtract(order.price())
                    : order.price().subtract(marketData.close());
            profitAndLoss = profitAndLoss.add(tradePriceChange.multiply(BigDecimal.valueOf(order.size())));
        }

        return profitAndLoss;
    }
}
