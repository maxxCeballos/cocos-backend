package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.shared.domain.money.Money;
import com.cocos.portfolio_service.user.domain.User;

import java.util.List;

public record OrderContext(
        User user,
        Instrument instrument,
        List<Order> orders,
        Money.ARS availableCash
) { }
