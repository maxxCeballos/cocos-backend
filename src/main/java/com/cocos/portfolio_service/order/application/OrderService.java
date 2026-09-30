package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
class OrderService implements IOrderService {
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;
    private final OrderRepository orderRepository;

    OrderService(UserRepository userRepository, InstrumentRepository instrumentRepository,
                 MarketDataRepository marketDataRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.instrumentRepository = instrumentRepository;
        this.marketDataRepository = marketDataRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public Order submit(OrderToSubmit command) {
        if (command == null || command.userId() == null || command.instrumentId() == null
                || command.side() == null || command.type() == null) {
            throw new InvalidOrderException("User, instrument, side, and type are required");
        }
        if (!userRepository.existsById(command.userId())) throw new UserNotFoundException(command.userId());
        instrumentRepository.findById(command.instrumentId())
                .orElseThrow(() -> new InstrumentNotFoundException(command.instrumentId()));
        if ((command.quantity() == null) == (command.amount() == null)) {
            throw new InvalidOrderException("Provide exactly one of quantity or amount");
        }
        if (command.side() == OrderSide.CASH_IN || command.side() == OrderSide.CASH_OUT) {
            throw new InvalidOrderException("Cash transfer orders cannot be submitted through this endpoint");
        }
        if (command.side() == OrderSide.SELL && command.amount() != null) {
            throw new InvalidOrderException("Amount-based orders are only supported for BUY orders");
        }
        if (command.quantity() != null && command.quantity() <= 0
                || command.amount() != null && command.amount().signum() <= 0) {
            throw new InvalidOrderException("Quantity and amount must be positive");
        }
        if (command.type() == OrderType.LIMIT && (command.price() == null || command.price().signum() <= 0)) {
            throw new InvalidOrderException("LIMIT orders require a positive price");
        }

        BigDecimal executionPrice = command.type() == OrderType.MARKET
                ? marketDataRepository.findLatestByInstrumentId(command.instrumentId())
                    .orElseThrow(() -> new MarketDataNotFoundException(command.instrumentId())).close()
                : command.price();
        if (executionPrice == null || executionPrice.signum() <= 0) {
            throw new InvalidOrderException("A positive instrument price is required");
        }

        long quantity;
        if (command.amount() != null) {
            quantity = command.amount().divide(executionPrice, 0, RoundingMode.DOWN).longValueExact();
            if (quantity == 0) throw new InvalidOrderException("Amount is insufficient to buy one whole share");
        } else {
            quantity = command.quantity();
        }
        if (quantity <= 0 || quantity > Integer.MAX_VALUE) {
            throw new InvalidOrderException("Order size must fit the database size range");
        }

        var filledOrders = orderRepository.findByUserIdAndStatus(command.userId(), OrderStatus.FILLED);
        BigDecimal cash = BigDecimal.ZERO;
        long heldQuantity = 0;
        for (var order : filledOrders) {
            switch (order.side()) {
                case CASH_IN -> cash = cash.add(BigDecimal.valueOf(order.quantity()));
                case CASH_OUT -> cash = cash.subtract(BigDecimal.valueOf(order.quantity()));
                case BUY -> {
                    cash = cash.subtract(order.price().multiply(BigDecimal.valueOf(order.quantity())));
                    if (order.instrumentId().equals(command.instrumentId())) heldQuantity += order.quantity();
                }
                case SELL -> {
                    cash = cash.add(order.price().multiply(BigDecimal.valueOf(order.quantity())));
                    if (order.instrumentId().equals(command.instrumentId())) heldQuantity -= order.quantity();
                }
            }
        }
        boolean rejected;
        if (command.side() == OrderSide.BUY) {
            var requiredCash = executionPrice.multiply(BigDecimal.valueOf(quantity));
            rejected = (command.amount() != null && command.amount().compareTo(cash) > 0)
                    || requiredCash.compareTo(cash) > 0;
        } else {
            rejected = quantity > heldQuantity;
        }

        var status = rejected ? OrderStatus.REJECTED
                : command.type() == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.NEW;
        var order = new Order(null, command.userId(), command.instrumentId(), command.side(), quantity,
                executionPrice, command.type(), status, Instant.now());
        return orderRepository.save(order);
    }
}
