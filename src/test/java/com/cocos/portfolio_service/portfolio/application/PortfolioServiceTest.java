package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private MarketDataRepository marketDataRepository;
    @Mock private ReturnService returnService;
    @InjectMocks private PortfolioService portfolioService;

    @Test
    void whenUserDoesNotExist_thenGetPortfolio_throwsUserNotFound() {
        // ARRANGE
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(UserNotFoundException.class, () -> portfolioService.getPortfolio(7L));
    }

    @Test
    void whenUserLookupFails_thenGetPortfolio_propagatesFailureAfterStartingOrdersLookup() {
        when(userRepository.findById(7L)).thenThrow(new IllegalStateException("user lookup failed"));
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> portfolioService.getPortfolio(7L));
        verify(orderRepository).findEffectiveOrdersByUserId(7L);
    }

    @Test
    void whenOrdersLookupFails_thenGetPortfolio_propagatesFailureAfterStartingUserLookup() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L))
                .thenThrow(new IllegalStateException("orders lookup failed"));

        assertThrows(IllegalStateException.class, () -> portfolioService.getPortfolio(7L));
        verify(userRepository).findById(7L);
    }

    @Test
    void whenInstrumentLookupFails_thenGetPortfolio_propagatesFailureAfterStartingMarketDataLookup() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of());
        when(instrumentRepository.findAllById(anyList()))
                .thenThrow(new IllegalStateException("instrument lookup failed"));
        when(marketDataRepository.findAllById(anyList())).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> portfolioService.getPortfolio(7L));
        verify(marketDataRepository).findAllById(List.of());
    }

    @Test
    void whenMarketDataLookupFails_thenGetPortfolio_propagatesFailureAfterStartingInstrumentLookup() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of());
        when(instrumentRepository.findAllById(anyList())).thenReturn(List.of());
        when(marketDataRepository.findAllById(anyList()))
                .thenThrow(new IllegalStateException("market data lookup failed"));

        assertThrows(IllegalStateException.class, () -> portfolioService.getPortfolio(7L));
        verify(instrumentRepository).findAllById(List.of());
    }

    @Test
    void whenUserHasNoFilledOrders_thenGetPortfolio_returnsEmptyZeroBalance() {
        // ARRANGE
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of());

        // ACT
        var result = portfolioService.getPortfolio(7L);

        // ASSERT
        assertEquals(BigDecimal.ZERO, result.totalAccountValue());
        assertEquals("$0.00", result.totalAccountValueLabel());
        assertEquals(BigDecimal.ZERO, result.availableCash());
        assertEquals("$0.00", result.availableCashLabel());
        assertEquals(BigDecimal.ZERO, result.onHoldCash());
        assertEquals("$0.00", result.onHoldCashLabel());
        assertEquals("$0.00", result.stockShareValueLabel());
        assertEquals(List.of(), result.instruments());
    }

    @Test
    void whenUserHasOnlyCashMovements_thenGetPortfolio_returnsCashOnlyAccount() {
        // ARRANGE
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of(
                order(OrderSide.CASH_IN, 500L, 65L, "1.00"),
                order(OrderSide.CASH_OUT, 125L, 65L, "1.00")));

        // ACT
        var result = portfolioService.getPortfolio(7L);

        // ASSERT
        assertEquals(new BigDecimal("375.00"), result.availableCash());
        assertEquals(BigDecimal.ZERO, result.onHoldCash());
        assertEquals(new BigDecimal("375.00"), result.totalAccountValue());
        assertEquals(List.of(), result.instruments());
    }

    @Test
    void whenUserHasBuyAndSellMovements_thenGetPortfolio_calculatesCashHoldingsAndDailyReturn() {
        // ARRANGE
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of(
                order(OrderSide.CASH_IN, 1000L, 65L, "1.00"),
                order(OrderSide.BUY, 2L, 3L, "100.00"),
                order(OrderSide.SELL, 1L, 3L, "150.00"),
                order(OrderSide.BUY, 2L, 3L, "10.00", OrderStatus.NEW)));
        when(instrumentRepository.findAllById(anyList())).thenReturn(List.of(instrument()));
        when(marketDataRepository.findAllById(anyList())).thenReturn(List.of(
                new MarketData(1L, 3L, new BigDecimal("120.00"), new BigDecimal("100.00"), LocalDate.now())));
        when(returnService.calculateDailyPositionReturn(anyList(), anyList()))
                .thenReturn(new BigDecimal("20.0000"));

        // ACT
        var result = portfolioService.getPortfolio(7L);

        // ASSERT
        assertEquals("AR$", result.currency());
        assertEquals("$1090.00", result.totalAccountValueLabel());
        assertEquals(new BigDecimal("950.00"), result.availableCash());
        assertEquals("$950.00", result.availableCashLabel());
        assertEquals(new BigDecimal("20.00"), result.onHoldCash());
        assertEquals("$20.00", result.onHoldCashLabel());
        assertEquals(new BigDecimal("1090.00"), result.totalAccountValue());
        assertEquals("$120.00", result.stockShareValueLabel());
        assertEquals(1, result.instruments().size());
        assertEquals(1L, result.instruments().getFirst().size());
        assertEquals(new BigDecimal("120.00"), result.instruments().getFirst().marketValue());
        assertEquals("$120.00", result.instruments().getFirst().marketValueLabel());
        assertEquals(new BigDecimal("20.0000"), result.instruments().getFirst().totalReturnPercent());
    }

    @Test
    void whenHoldingHasNoMarketData_thenGetPortfolio_throwsMarketDataNotFound() {
        // ARRANGE
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(orderRepository.findEffectiveOrdersByUserId(7L))
                .thenReturn(List.of(order(OrderSide.BUY, 2L, 3L, "100.00")));
        when(instrumentRepository.findAllById(anyList())).thenReturn(List.of(instrument()));
        when(marketDataRepository.findAllById(anyList())).thenReturn(List.of());

        // ACT & ASSERT
        assertThrows(MarketDataNotFoundException.class, () -> portfolioService.getPortfolio(7L));
    }

    private Order order(OrderSide side, long quantity, long instrumentId, String price) {
        return order(side, quantity, instrumentId, price, OrderStatus.FILLED);
    }

    private Order order(OrderSide side, long quantity, long instrumentId, String price, OrderStatus status) {
        return new Order(1L, 7L, instrumentId, side, quantity, new BigDecimal(price), OrderType.MARKET,
                status, LocalDateTime.of(2023, 7, 13, 12, 0));
    }

    private User user() {
        return new User(7L, "user@example.com", "account-7");
    }

    private Instrument instrument() {
        return new Instrument(3L, "GGAL", "Grupo Galicia", "ACCIONES", null);
    }
}
