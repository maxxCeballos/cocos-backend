package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.order.application.OrderService;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.domain.money.Money;
import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class PortfolioService implements IPortfolioService {
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;
    private final ReturnService returnService;

    PortfolioService(
            UserRepository userRepository,
            OrderService orderService,
            OrderRepository orderRepository,
            InstrumentRepository instrumentRepository,
            MarketDataRepository marketDataRepository,
            ReturnService returnService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.marketDataRepository = marketDataRepository;
        this.returnService = returnService;
    }

    public Portfolio getPortfolio(Long userId) {
        PortfolioContext context = buildContext(userId);

        Money.ARS availableCash = orderService.calculateAvailableCash(context.orders());
        Money.ARS onHoldCash = calculateOnHoldCash(context.orders());
        Money.ARS totalStockShareValue = calculateTotalStockShareValue(context.orders(), context.marketsData());

        Money.ARS totalAccountValue = availableCash.add(onHoldCash).add(totalStockShareValue);
        List<Portfolio.Instrument> instrumentInfoAggregated = aggregateInstrumentInfo(context.orders(), context.marketsData(), context.instruments());

        return new Portfolio(
                Money.ARS.CURRENCY_LABEL,
                totalAccountValue.value(), totalAccountValue.toString(),
                availableCash.value(), availableCash.toString(),
                onHoldCash.value(), onHoldCash.toString(),
                totalStockShareValue.value(), totalStockShareValue.toString(),
                instrumentInfoAggregated);
    }

    private PortfolioContext buildContext(Long userId) {
        CompletableFuture<Optional<User>> userFuture = CompletableFuture.supplyAsync(
                () -> userRepository.findById(userId));
        CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(
                () -> orderRepository.findEffectiveOrdersByUserId(userId));
        awaitAll(userFuture, ordersFuture);

        Optional<User> userOpt = userFuture.join();
        List<Order> orders = ordersFuture.join();

        if(userOpt.isEmpty()) throw new UserNotFoundException(userId);
        List<Long> instrumentIds = orders.stream().map(Order::instrumentId).toList();

        CompletableFuture<List<Instrument>> instrumentsFuture = CompletableFuture.supplyAsync(
                () -> instrumentRepository.findAllById(instrumentIds));
        CompletableFuture<List<MarketData>> marketsDataFuture = CompletableFuture.supplyAsync(
                () -> marketDataRepository.findAllByInstrumentId(instrumentIds));
        awaitAll(instrumentsFuture, marketsDataFuture);

        List<Instrument> instruments = instrumentsFuture.join();
        List<MarketData> marketsData = marketsDataFuture.join();

        PortfolioContext context = new PortfolioContext(userOpt.get(), orders, instruments, marketsData);

        return context;
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

    // STOCK SHARE: ✅
    private Money.ARS calculateTotalStockShareValue(List<Order> orders, List<MarketData> marketsData) {
        Money.ARS totalStockShareValue = new Money.ARS(BigDecimal.ZERO);

        List<Order> ordersToCalculate = orders.stream().filter(order -> order.toShareSwapped() || order.toCashSwapped()).toList();
        Map<Long, MarketData> instrumentToMarketdataMap = getLastInstrumentMarketData(marketsData);

        Map<Long, Long> instrumentToCantMap = calculateInstrumentCantMap(ordersToCalculate);

        for (Map.Entry<Long, Long> instrumentCant : instrumentToCantMap.entrySet()) {
            MarketData instMarketdata = instrumentToMarketdataMap.get(instrumentCant.getKey());
            Money.ARS totalInstrumentValue = new Money.ARS(instMarketdata.close())
                    .multiply(BigDecimal.valueOf(instrumentCant.getValue()));
            totalStockShareValue = totalStockShareValue.add(totalInstrumentValue);
        }

        return totalStockShareValue;
    }

    // ON-HOLD-CASH: ✅
    private Money.ARS calculateOnHoldCash(List<Order> orders) {
        return orders.stream()
                .filter(Order::isCashOnHold)
                .map(order -> new Money.ARS(order.price()).multiply(BigDecimal.valueOf(order.size())))
                .reduce(new Money.ARS(BigDecimal.ZERO), Money.ARS::add);
    }

    private List<Portfolio.Instrument> aggregateInstrumentInfo(List<Order> orders, List<MarketData> marketsData, List<Instrument> instruments) {
        ArrayList<Portfolio.Instrument> instrumentInfoAggregated = new ArrayList<>();

        List<Order> ordersToCalculate = orders.stream().filter(order -> order.status().equals(OrderStatus.FILLED) && !List.of(OrderSide.CASH_IN, OrderSide.CASH_OUT).contains(order.side())).toList();
        Map<Long, MarketData> instrumentToMarketdataMap = getLastInstrumentMarketData(marketsData);
        Map<Long, Instrument> instrumentsMap = instruments.stream().collect(Collectors.toMap(Instrument::id, Function.identity()));

        Map<Long, Long> instrumentToCantMap = calculateInstrumentCantMap(ordersToCalculate);

        Set<Long> instrumentIds = ordersToCalculate.stream()
                .map(Order::instrumentId)
                .collect(Collectors.toSet());


        for(Long instrumentId: instrumentIds) {
            Instrument instrument = instrumentsMap.get(instrumentId);
            MarketData marketData = instrumentToMarketdataMap.get(instrumentId);
            Long size = instrumentToCantMap.get(instrumentId);
            List<MarketData> instrumentMarketData = marketsData.stream()
                    .filter(data -> Objects.equals(data.instrumentId(), instrumentId))
                    .toList();
            List<Order> instrumentOrders = ordersToCalculate.stream()
                    .filter(order -> Objects.equals(order.instrumentId(), instrumentId))
                    .toList();

            Money.ARS marketValue = new Money.ARS(marketData.close()).multiply(BigDecimal.valueOf(size));
            instrumentInfoAggregated.add(new Portfolio.Instrument(
                    instrumentId,
                    instrument.ticker(),
                    instrument.name(),
                    size,
                    marketValue.value(),
                    marketValue.toString(),
                    returnService.calculateDailyPositionReturn(instrumentMarketData, instrumentOrders)
            ));
        }

        return instrumentInfoAggregated;
    }

    private Map<Long, Long> calculateInstrumentCantMap(List<Order> ordersToEvaluate) {
        Map<Long, Long> instrumentCantMap = new HashMap<>();

        for (Order order: ordersToEvaluate) {
            if(!instrumentCantMap.containsKey(order.instrumentId())) {
                Long firstCant = order.isShare() ? order.size() : -order.size();
                instrumentCantMap.put(order.instrumentId(), firstCant);
            } else {
                Long valueToModify = instrumentCantMap.get(order.instrumentId());
                valueToModify = order.isShare() ? (valueToModify + order.size()) : (valueToModify - order.size());
                instrumentCantMap.put(order.instrumentId(), valueToModify);
            }
        }
        return instrumentCantMap;
    }

    private Map<Long, MarketData> getLastInstrumentMarketData(List<MarketData> marketsData) {
        return marketsData.stream().collect(Collectors.toMap(
                MarketData::instrumentId, entity -> entity,
                BinaryOperator.maxBy(Comparator.comparing(MarketData::date))
        ));
    }
}
