package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.user.domain.User;

import java.util.List;

public record PortfolioContext(
        User user,
        List<Order> orders,
        List<Instrument> instruments,
        List<MarketData> marketsData
) { }
