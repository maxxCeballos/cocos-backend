package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import com.cocos.portfolio_service.user.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashInSideTest {
    private final CashInSide cashInSide = new CashInSide();
    private final OrderContext currencyContext = new OrderContext(
            new User(7L, "user@example.com", "account-7"),
            new Instrument(3L, "ARS", "Pesos", "MONEDA"));

    @Test
    void whenCashInIsValid_thenCreatesFilledOrderAtPriceOne() {
        LocalDateTime beforeSubmission = LocalDateTime.now();

        Order result = cashInSide.submit(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, 500L, new BigDecimal("1.00")));

        LocalDateTime afterSubmission = LocalDateTime.now();
        assertEquals(7L, result.userId());
        assertEquals(3L, result.instrumentId());
        assertEquals(OrderSide.CASH_IN, result.side());
        assertEquals(500L, result.size());
        assertEquals(BigDecimal.ONE, result.price());
        assertEquals(OrderType.MARKET, result.type());
        assertEquals(OrderStatus.FILLED, result.status());
        assertNotNull(result.datetime());
        assertTrue(!result.datetime().isBefore(beforeSubmission) && !result.datetime().isAfter(afterSubmission));
    }

    @Test
    void whenCashInUsesNonMarketType_thenRejectsOrder() {
        assertInvalidOrder(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.LIMIT, 500L, BigDecimal.ONE));
    }

    @Test
    void whenCashInPriceIsNotOneOrMissing_thenRejectsOrder() {
        assertInvalidOrder(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, 500L, new BigDecimal("1.01")));
        assertInvalidOrder(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, 500L, null));
    }

    @Test
    void whenInstrumentIsNotCurrency_thenRejectsOrder() {
        OrderContext stockContext = new OrderContext(currencyContext.user(),
                new Instrument(4L, "GGAL", "Grupo Galicia", "ACCIONES"));

        assertInvalidOrder(stockContext,
                new OrderToSubmit(4L, OrderSide.CASH_IN, OrderType.MARKET, 500L, BigDecimal.ONE));
    }

    @Test
    void whenSizeIsMissingOrNotPositive_thenRejectsOrder() {
        assertInvalidOrder(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, null, BigDecimal.ONE));
        assertInvalidOrder(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, 0L, BigDecimal.ONE));
        assertInvalidOrder(currencyContext,
                new OrderToSubmit(3L, OrderSide.CASH_IN, OrderType.MARKET, -1L, BigDecimal.ONE));
    }

    private void assertInvalidOrder(OrderContext context, OrderToSubmit command) {
        assertThrows(InvalidOrderException.class, () -> cashInSide.submit(context, command));
    }
}
