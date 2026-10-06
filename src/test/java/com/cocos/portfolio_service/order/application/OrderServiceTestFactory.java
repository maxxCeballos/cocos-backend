package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.application.side_strategy.BuySide;
import com.cocos.portfolio_service.order.application.side_strategy.CashInSide;
import com.cocos.portfolio_service.order.application.side_strategy.CashOutSide;
import com.cocos.portfolio_service.order.application.side_strategy.SellSide;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService;
import com.cocos.portfolio_service.user.domain.UserRepository;

import java.util.Map;

/** Test-only constructor bridge for the package-private service constructor. */
public final class OrderServiceTestFactory {
    private OrderServiceTestFactory() { }

    public static OrderService create(UserRepository users, OrderRepository orders,
                                      InstrumentRepository instruments, MarketDataRepository marketData,
                                      SharedLockService locks) {
        return new OrderService(users, orders, instruments,
                Map.of("BUY", new BuySide(marketData), "SELL", new SellSide(marketData),
                        "CASH_IN", new CashInSide(), "CASH_OUT", new CashOutSide()), locks);
    }
}
