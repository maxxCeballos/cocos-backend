package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.application.side_strategy.SideStrategy;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.domain.money.Money;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService.LockHandle;
import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class OrderService implements IOrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;
    private final Map<String, SideStrategy> strategies;
    private final SharedLockService lockService;

    OrderService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            InstrumentRepository instrumentRepository,
            MarketDataRepository marketDataRepository,
            Map<String, SideStrategy> strategies,
            SharedLockService lockService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.marketDataRepository = marketDataRepository;
        this.strategies = strategies;
        this.lockService = lockService;
    }

    @Override
    @Transactional
    public Order submit(Long userId, OrderToSubmit orderToSubmit) {

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) throw new UserNotFoundException(userId);

        Optional<Instrument> instOpt = instrumentRepository.findById(orderToSubmit.instrumentId());
        if(instOpt.isEmpty()) throw new InstrumentNotFoundException(orderToSubmit.instrumentId());

        SideStrategy strategy = strategies.get(orderToSubmit.side().toString());
        OrderContext context = buildContext(userId, orderToSubmit.instrumentId());

        LockHandle lock = lockService.acquireForUser(userId);

        try {
            Order orderToSave = strategy.submit(context, orderToSubmit);
            Order orderSaved = orderRepository.save(orderToSave);
            return orderSaved;
        } finally {
            lock.release();
        }
    }

    public OrderContext buildContext(Long userId, Long instrumentId) {
        CompletableFuture<Optional<User>> userFuture = CompletableFuture.supplyAsync(
                () -> userRepository.findById(userId));
        CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(
                () -> orderRepository.findEffectiveOrdersByUserId(userId));

        awaitAll(userFuture, ordersFuture);

        Optional<User> userOpt = userFuture.join();
        List<Order> orders = ordersFuture.join();

        if(userOpt.isEmpty()) throw new UserNotFoundException(userId);

        CompletableFuture<Optional<Instrument>> instrumentFuture = CompletableFuture.supplyAsync(
                () -> instrumentRepository.findById(instrumentId));
        CompletableFuture<Optional<MarketData>> marketDataFuture = CompletableFuture.supplyAsync(
                () -> marketDataRepository.findLatestByInstrumentId(instrumentId));
        awaitAll(instrumentFuture,marketDataFuture);

        Optional<Instrument> instOpt = instrumentFuture.join();
        Optional<MarketData> marketDataOpt = marketDataFuture.join();
        if(instOpt.isEmpty()) throw new InstrumentNotFoundException(instrumentId);
        if(marketDataOpt.isEmpty()) throw new MarketDataNotFoundException(instrumentId);

        Money.ARS availableCash = calculateAvailableCash(orders);

        return new OrderContext(userOpt.get(), instOpt.get(), orders, marketDataOpt.get(), availableCash);
    }

    private void awaitAll(CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw exception;
        }
    }

    // CASH: ✅
    public Money.ARS calculateAvailableCash(List<Order> orders) {
        Money.ARS cash = new Money.ARS(BigDecimal.ZERO);

        List<Order> ordersToCalculate = orders.stream()
                .filter(order -> !order.isShareOnHold() && !order.isCashOnHold())
                .toList();

        for (Order order: ordersToCalculate) {
            Money.ARS valueToOperate = new Money.ARS(order.price())
                    .multiply(BigDecimal.valueOf(order.size()));

            if(order.isCashIn() || order.toCashSwapped()) {
                cash = cash.add(valueToOperate);
                continue;
            }

            cash = cash.subtract(valueToOperate);
        }

        return cash;
    }
}
