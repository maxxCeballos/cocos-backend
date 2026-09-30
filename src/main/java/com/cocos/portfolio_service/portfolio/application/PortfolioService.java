package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.InvalidMarketDataException;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.application.OrderRepository;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
class PortfolioService implements IPortfolioService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;

    PortfolioService(UserRepository userRepository, OrderRepository orderRepository,
                     InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.marketDataRepository = marketDataRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        var filledOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.FILLED);
        BigDecimal cash = BigDecimal.ZERO;
        Map<Long, Position> positions = new LinkedHashMap<>();
        for (var order : filledOrders) {
            switch (order.side()) {
                case CASH_IN -> cash = cash.add(BigDecimal.valueOf(order.quantity()));
                case CASH_OUT -> cash = cash.subtract(BigDecimal.valueOf(order.quantity()));
                case BUY -> {
                    cash = cash.subtract(order.price().multiply(BigDecimal.valueOf(order.quantity())));
                    positions.computeIfAbsent(order.instrumentId(), ignored -> new Position()).quantity += order.quantity();
                }
                case SELL -> {
                    cash = cash.add(order.price().multiply(BigDecimal.valueOf(order.quantity())));
                    positions.computeIfAbsent(order.instrumentId(), ignored -> new Position()).quantity -= order.quantity();
                }
            }
        }

        var holdings = new ArrayList<Portfolio.Instrument>();
        BigDecimal holdingsValue = BigDecimal.ZERO;
        for (var entry : positions.entrySet()) {
            var position = entry.getValue();
            if (position.quantity <= 0) continue;
            var instrument = instrumentRepository.findById(entry.getKey())
                    .orElseThrow(() -> new InstrumentNotFoundException(entry.getKey()));
            var marketData = marketDataRepository.findLatestByInstrumentId(entry.getKey())
                    .orElseThrow(() -> new MarketDataNotFoundException(entry.getKey()));
            if (marketData.close() == null || marketData.close().signum() <= 0) {
                throw new InvalidMarketDataException(entry.getKey());
            }
            var marketValue = marketData.close().multiply(BigDecimal.valueOf(position.quantity));
            holdingsValue = holdingsValue.add(marketValue);
            var dailyReturn = marketData.previousClose() == null || marketData.previousClose().signum() == 0
                    ? BigDecimal.ZERO
                    : marketData.close().subtract(marketData.previousClose())
                    .multiply(BigDecimal.valueOf(100)).divide(marketData.previousClose(), 4, RoundingMode.HALF_UP);
            holdings.add(new Portfolio.Instrument(instrument.id(), instrument.ticker(), instrument.name(),
                    position.quantity, marketValue, dailyReturn));
        }
        return new Portfolio(cash.add(holdingsValue), cash, holdings);
    }

    private static final class Position {
        private long quantity;
    }
}
