package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import com.cocos.portfolio_service.shared.domain.money.Money;
import com.cocos.portfolio_service.user.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CashInSideTest {
    private final CashInSide strategy = new CashInSide();
    private final OrderContext currencyContext = new OrderContext(
            new User(7L, "user@example.com", "7"),
            new Instrument(3L, "ARS", "Pesos", InstrumentType.MONEDA),
            List.of(), null, new Money.ARS(BigDecimal.ZERO));

    @Test
    void cashInOnCurrencyInstrumentCreatesFilledMarketOrderAtUnitPrice() {
        Order result = strategy.submit(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, 500L,
                        new Money.ARS(new BigDecimal("999.00")), new Money.ARS(BigDecimal.ZERO)));

        assertEquals(7L, result.userId());
        assertEquals(3L, result.instrumentId());
        assertEquals(OrderSide.CASH_IN, result.side());
        assertEquals(500L, result.size());
        assertEquals(BigDecimal.ONE, result.price());
        assertEquals(OrderType.MARKET, result.type());
        assertEquals(OrderStatus.FILLED, result.status());
        assertNotNull(result.datetime());
    }

    @Test
    void cashInRejectsLimitOrdersAndNonCurrencyInstruments() {
        assertThrows(InvalidOrderException.class, () -> strategy.submit(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.LIMIT, 500L,
                        new Money.ARS(BigDecimal.ONE), new Money.ARS(BigDecimal.ZERO))));

        var stockContext = new OrderContext(currencyContext.user(),
                new Instrument(4L, "GGAL", "Grupo Galicia", InstrumentType.ACCIONES),
                List.of(), null, new Money.ARS(BigDecimal.ZERO));
        assertThrows(InvalidOrderException.class, () -> strategy.submit(stockContext,
                new OrderToSubmit(4L, OrderSide.CASH_IN, OrderType.MARKET, 500L,
                        new Money.ARS(BigDecimal.ZERO), new Money.ARS(BigDecimal.ZERO))));
    }
}
