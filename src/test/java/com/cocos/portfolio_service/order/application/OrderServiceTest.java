package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import com.cocos.portfolio_service.user.domain.UserRepository;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private InstrumentRepository instrumentRepository;
    @Mock private MarketDataRepository marketDataRepository;
    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.existsById(7L)).thenReturn(true);
        lenient().when(instrumentRepository.findById(3L)).thenReturn(Optional.of(
                new Instrument(3L, "GGAL", "Grupo Galicia", "ACCIONES", null)));
        lenient().when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.of(
                new MarketData(1L, 3L, new BigDecimal("100.00"), new BigDecimal("95.00"), LocalDate.now())));
        lenient().when(orderRepository.findEffectiveOrdersByUserId(7L, OrderStatus.FILLED)).thenReturn(List.of(
                new Order(1L, 7L, 65L, OrderSide.CASH_IN, 1000L, BigDecimal.ONE,
                        OrderType.MARKET, OrderStatus.FILLED, Instant.EPOCH)));
        lenient().when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenMarketBuyFitsAvailableCash_thenSubmit_savesFilledOrderAtLatestClose() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 2L, null, null);

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(2L, result.quantity());
        assertEquals(new BigDecimal("100.00"), result.price());
        assertEquals(OrderStatus.FILLED, result.status());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void whenLimitBuyFitsAvailableCash_thenSubmit_savesNewOrderAtLimitPrice() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.LIMIT, 2L, null, new BigDecimal("90.00"));

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(new BigDecimal("90.00"), result.price());
        assertEquals(OrderStatus.NEW, result.status());
    }

    @Test
    void whenBuyUsesAmount_thenSubmit_convertsAmountToWholeShares() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, null, new BigDecimal("250.00"), null);

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(2L, result.quantity());
        assertEquals(OrderStatus.FILLED, result.status());
    }

    @Test
    void whenBuyExceedsAvailableCash_thenSubmit_savesRejectedOrder() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 11L, null, null);

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(11L, result.quantity());
    }

    @Test
    void whenBuyExactlyMatchesAvailableCash_thenSubmit_savesFilledOrder() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 10L, null, null);

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(10L, result.quantity());
    }

    @Test
    void whenSellExceedsAvailableHoldings_thenSubmit_savesRejectedOrder() {
        // ARRANGE
        var command = command(OrderSide.SELL, OrderType.MARKET, 1L, null, null);

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(OrderStatus.REJECTED, result.status());
    }

    @Test
    void whenSellExactlyMatchesAvailableHoldings_thenSubmit_savesFilledOrder() {
        // ARRANGE
        when(orderRepository.findEffectiveOrdersByUserId(7L, OrderStatus.FILLED)).thenReturn(List.of(
                new Order(1L, 7L, 65L, OrderSide.CASH_IN, 1000L, BigDecimal.ONE,
                        OrderType.MARKET, OrderStatus.FILLED, Instant.EPOCH),
                new Order(2L, 7L, 3L, OrderSide.BUY, 1L, new BigDecimal("80.00"),
                        OrderType.MARKET, OrderStatus.FILLED, Instant.EPOCH)));
        var command = command(OrderSide.SELL, OrderType.MARKET, 1L, null, null);

        // ACT
        var result = orderService.submit(command);

        // ASSERT
        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(1L, result.quantity());
    }

    @Test
    void whenUserDoesNotExist_thenSubmit_throwsUserNotFound() {
        // ARRANGE
        when(userRepository.existsById(7L)).thenReturn(false);

        // ACT & ASSERT
        assertThrows(UserNotFoundException.class,
                () -> orderService.submit(command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenInstrumentDoesNotExist_thenSubmit_throwsInstrumentNotFound() {
        // ARRANGE
        when(instrumentRepository.findById(3L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(InstrumentNotFoundException.class,
                () -> orderService.submit(command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenMarketDataDoesNotExist_thenSubmit_throwsMarketDataNotFound() {
        // ARRANGE
        when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(MarketDataNotFoundException.class,
                () -> orderService.submit(command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenQuantityAndAmountAreBothMissing_thenSubmit_throwsInvalidOrder() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, null, null, null);

        // ACT & ASSERT
        assertThrows(InvalidOrderException.class, () -> orderService.submit(command));
    }

    @Test
    void whenSavingOrderFails_thenSubmit_propagatesPersistenceFailure() {
        // ARRANGE
        when(orderRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        // ACT & ASSERT
        assertThrows(IllegalStateException.class,
                () -> orderService.submit(command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    private OrderToSubmit command(OrderSide side, OrderType type, Long quantity, BigDecimal amount, BigDecimal price) {
        return new OrderToSubmit(7L, 3L, side, type, quantity, amount, price);
    }
}
