package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import com.cocos.portfolio_service.user.domain.UserRepository;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.domain.money.Money;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService;
import com.cocos.portfolio_service.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

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
    @Mock private SharedLockService lockService;
    @Mock private SharedLockService.LockHandle lockHandle;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(userRepository, orderRepository, instrumentRepository,
                Map.of("BUY", new com.cocos.portfolio_service.order.application.side_strategy.BuySide(marketDataRepository),
                        "SELL", new com.cocos.portfolio_service.order.application.side_strategy.SellSide(marketDataRepository),
                        "CASH_IN", new com.cocos.portfolio_service.order.application.side_strategy.CashInSide(),
                        "CASH_OUT", new com.cocos.portfolio_service.order.application.side_strategy.CashOutSide()),
                lockService);
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(new User(7L, "user@example.com", "7")));
        lenient().when(instrumentRepository.findById(3L)).thenReturn(Optional.of(
                new Instrument(3L, "GGAL", "Grupo Galicia", InstrumentType.ACCIONES)));
        lenient().when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.of(
                new MarketData(1L, 3L, new BigDecimal("100.00"), new BigDecimal("95.00"), LocalDate.now())));
        lenient().when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of(
                new Order(1L, 7L, 65L, OrderSide.CASH_IN, 1000L, BigDecimal.ONE,
                        OrderType.MARKET, OrderStatus.FILLED, LocalDateTime.of(2023, 7, 13, 12, 0))));
        lenient().when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(lockService.acquireForUser(7L)).thenReturn(lockHandle);
    }

    @Test
    void whenMarketBuyFitsAvailableCash_thenSubmit_savesFilledOrderAtLatestClose() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 2L, null, null);

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(2L, result.size());
        assertEquals(new BigDecimal("100.00"), result.price());
        assertEquals(OrderStatus.FILLED, result.status());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void whenLimitBuyFitsAvailableCash_thenSubmit_savesNewOrderAtLimitPrice() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.LIMIT, 2L, null, new BigDecimal("90.00"));

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(new BigDecimal("90.00"), result.price());
        assertEquals(OrderStatus.NEW, result.status());
    }

    @Test
    void whenBuyUsesAmount_thenSubmit_convertsAmountToWholeShares() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 0L, new BigDecimal("250.00"), null);

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(2L, result.size());
        assertEquals(OrderStatus.FILLED, result.status());
    }

    @Test
    void whenBuyHasNoSizeAndNoBudget_thenSubmit_throwsInvalidOrder() {
        var command = command(OrderSide.BUY, OrderType.MARKET, 0L, BigDecimal.ZERO, null);

        assertThrows(InvalidOrderException.class, () -> orderService.submit(7L, command));
    }

    @Test
    void whenBuyExceedsAvailableCash_thenSubmit_savesRejectedOrder() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 11L, null, null);

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(11L, result.size());
    }

    @Test
    void whenBuyExactlyMatchesAvailableCash_thenSubmit_savesFilledOrder() {
        // ARRANGE
        var command = command(OrderSide.BUY, OrderType.MARKET, 10L, null, null);

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(10L, result.size());
    }

    @Test
    void whenCalculatingAvailableCash_thenNewBuyIsReservedAndNewSellIsIgnored() {
        var orders = List.of(
                cashOrder(OrderSide.CASH_IN, 1000L, "1.00", OrderStatus.FILLED),
                cashOrder(OrderSide.BUY, 2L, "100.00", OrderStatus.FILLED),
                cashOrder(OrderSide.SELL, 1L, "150.00", OrderStatus.FILLED),
                cashOrder(OrderSide.BUY, 2L, "10.00", OrderStatus.NEW),
                cashOrder(OrderSide.SELL, 100L, "50.00", OrderStatus.NEW));

        var availableCash = orderService.calculateAvailableCash(orders);

        assertEquals(new BigDecimal("930.00"), availableCash.value());
    }

    @Test
    void whenSellExceedsAvailableHoldings_thenSubmit_savesRejectedOrder() {
        // ARRANGE
        var command = command(OrderSide.SELL, OrderType.MARKET, 1L, null, null);

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(OrderStatus.REJECTED, result.status());
    }

    @Test
    void whenSellExactlyMatchesAvailableHoldings_thenSubmit_savesFilledOrder() {
        // ARRANGE
        when(orderRepository.findEffectiveOrdersByUserId(7L)).thenReturn(List.of(
                new Order(1L, 7L, 65L, OrderSide.CASH_IN, 1000L, BigDecimal.ONE,
                        OrderType.MARKET, OrderStatus.FILLED, LocalDateTime.of(2023, 7, 13, 12, 0)),
                new Order(2L, 7L, 3L, OrderSide.BUY, 1L, new BigDecimal("80.00"),
                        OrderType.MARKET, OrderStatus.FILLED, LocalDateTime.of(2023, 7, 13, 12, 0))));
        var command = command(OrderSide.SELL, OrderType.MARKET, 1L, null, null);

        // ACT
        var result = orderService.submit(7L, command);

        // ASSERT
        assertEquals(OrderStatus.FILLED, result.status());
        assertEquals(1L, result.size());
    }

    @Test
    void whenUserDoesNotExist_thenSubmit_throwsUserNotFound() {
        // ARRANGE
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(UserNotFoundException.class,
                () -> orderService.submit(7L, command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenInstrumentDoesNotExist_thenSubmit_throwsInstrumentNotFound() {
        // ARRANGE
        when(instrumentRepository.findById(3L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(InstrumentNotFoundException.class,
                () -> orderService.submit(7L, command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenMarketDataDoesNotExist_thenSubmit_throwsMarketDataNotFound() {
        // ARRANGE
        when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(MarketDataNotFoundException.class,
                () -> orderService.submit(7L, command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenMarketDataDoesNotExist_thenSell_throwsMarketDataNotFound() {
        when(marketDataRepository.findLatestByInstrumentId(3L)).thenReturn(Optional.empty());

        assertThrows(MarketDataNotFoundException.class,
                () -> orderService.submit(7L, command(OrderSide.SELL, OrderType.MARKET, 1L, null, null)));
    }

    @Test
    void whenCashInSubmitted_thenSubmit_doesNotLookupMarketData() {
        when(instrumentRepository.findById(65L)).thenReturn(Optional.of(
                new Instrument(65L, "ARS", "Pesos", InstrumentType.MONEDA)));

        var result = orderService.submit(7L, new OrderToSubmit(65L, OrderSide.CASH_IN, OrderType.MARKET, 100L,
                new Money.ARS(BigDecimal.ZERO), new Money.ARS(BigDecimal.ZERO)));

        assertEquals(BigDecimal.ONE, result.price());
        assertEquals(OrderStatus.FILLED, result.status());
        org.mockito.Mockito.verifyNoInteractions(marketDataRepository);
    }

    @Test
    void whenCashOutSubmitted_thenSubmit_doesNotLookupMarketData() {
        when(instrumentRepository.findById(65L)).thenReturn(Optional.of(
                new Instrument(65L, "ARS", "Pesos", InstrumentType.MONEDA)));

        var result = orderService.submit(7L, new OrderToSubmit(65L, OrderSide.CASH_OUT, OrderType.MARKET, 100L,
                new Money.ARS(BigDecimal.ZERO), new Money.ARS(BigDecimal.ZERO)));

        assertEquals(BigDecimal.ONE, result.price());
        org.mockito.Mockito.verifyNoInteractions(marketDataRepository);
    }

    @Test
    void whenSavingOrderFails_thenSubmit_propagatesPersistenceFailure() {
        // ARRANGE
        when(orderRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        // ACT & ASSERT
        assertThrows(IllegalStateException.class,
                () -> orderService.submit(7L, command(OrderSide.BUY, OrderType.MARKET, 1L, null, null)));
    }

    private OrderToSubmit command(OrderSide side, OrderType type, Long quantity, BigDecimal amount, BigDecimal price) {
        return new OrderToSubmit(3L, side, type, quantity,
                new Money.ARS(price == null ? BigDecimal.ZERO : price),
                new Money.ARS(amount == null ? BigDecimal.ZERO : amount));
    }

    private Order cashOrder(OrderSide side, long size, String price, OrderStatus status) {
        return new Order(1L, 7L, 3L, side, size, new BigDecimal(price), OrderType.MARKET,
                status, LocalDateTime.of(2023, 7, 13, 12, 0));
    }
}
