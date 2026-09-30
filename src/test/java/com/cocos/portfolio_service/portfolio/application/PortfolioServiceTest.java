package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.application.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private MarketDataRepository marketDataRepository;
    @InjectMocks private PortfolioService portfolioService;

    @Test
    void whenUserDoesNotExist_thenGetPortfolio_throwsUserNotFound() {
        // ARRANGE
        when(userRepository.existsById(7L)).thenReturn(false);

        // ACT & ASSERT
        assertThrows(UserNotFoundException.class, () -> portfolioService.getPortfolio(7L));
    }

    @Test
    void whenUserHasNoFilledOrders_thenGetPortfolio_returnsEmptyZeroBalance() {
        // ARRANGE
        when(userRepository.existsById(7L)).thenReturn(true);
        when(orderRepository.findByUserIdAndStatus(7L, OrderStatus.FILLED)).thenReturn(List.of());

        // ACT
        var result = portfolioService.getPortfolio(7L);

        // ASSERT
        assertEquals(BigDecimal.ZERO, result.totalAccountValue());
        assertEquals(BigDecimal.ZERO, result.availableCash());
        assertEquals(List.of(), result.instruments());
    }

    @Test
    void whenUserHasOnlyCashMovements_thenGetPortfolio_returnsCashOnlyAccount() {
        // ARRANGE
        when(userRepository.existsById(7L)).thenReturn(true);
        when(orderRepository.findByUserIdAndStatus(7L, OrderStatus.FILLED)).thenReturn(List.of(
                order(OrderSide.CASH_IN, 500L, 65L, "1.00"),
                order(OrderSide.CASH_OUT, 125L, 65L, "1.00")));

        // ACT
        var result = portfolioService.getPortfolio(7L);

        // ASSERT
        assertEquals(new BigDecimal("375"), result.availableCash());
        assertEquals(new BigDecimal("375"), result.totalAccountValue());
        assertEquals(List.of(), result.instruments());
    }

    @Test
    void whenUserHasBuyAndSellMovements_thenGetPortfolio_calculatesCashHoldingsAndDailyReturn() {
        // ARRANGE
        when(userRepository.existsById(7L)).thenReturn(true);
        when(orderRepository.findByUserIdAndStatus(7L, OrderStatus.FILLED)).thenReturn(List.of(
                order(OrderSide.CASH_IN, 1000L, 65L, "1.00"),
                order(OrderSide.BUY, 2L, 3L, "100.00"),
                order(OrderSide.SELL, 1L, 3L, "150.00")));
        when(instrumentRepository.findById(3L)).thenReturn(Optional.of(
                new Instrument(3L, "GGAL", "Grupo Galicia", "ACCIONES", null)));
        when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.of(
                new MarketData(1L, 3L, new BigDecimal("120.00"), new BigDecimal("100.00"), LocalDate.now())));

        // ACT
        var result = portfolioService.getPortfolio(7L);

        // ASSERT
        assertEquals(new BigDecimal("950.00"), result.availableCash());
        assertEquals(new BigDecimal("1070.00"), result.totalAccountValue());
        assertEquals(1, result.instruments().size());
        assertEquals(1L, result.instruments().getFirst().quantity());
        assertEquals(new BigDecimal("120.00"), result.instruments().getFirst().marketValue());
        assertEquals(new BigDecimal("20.0000"), result.instruments().getFirst().totalReturnPercent());
    }

    @Test
    void whenHoldingHasNoMarketData_thenGetPortfolio_throwsMarketDataNotFound() {
        // ARRANGE
        when(userRepository.existsById(7L)).thenReturn(true);
        when(orderRepository.findByUserIdAndStatus(7L, OrderStatus.FILLED))
                .thenReturn(List.of(order(OrderSide.BUY, 2L, 3L, "100.00")));
        when(instrumentRepository.findById(3L)).thenReturn(Optional.of(
                new Instrument(3L, "GGAL", "Grupo Galicia", "ACCIONES", null)));
        when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(MarketDataNotFoundException.class, () -> portfolioService.getPortfolio(7L));
    }

    private Order order(OrderSide side, long quantity, long instrumentId, String price) {
        return new Order(1L, 7L, instrumentId, side, quantity, new BigDecimal(price), OrderType.MARKET,
                OrderStatus.FILLED, Instant.EPOCH);
    }
}
