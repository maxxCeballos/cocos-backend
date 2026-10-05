package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
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
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;
    private final ReturnService returnService;

    PortfolioService(UserRepository userRepository, OrderRepository orderRepository,
                     InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository,
                     ReturnService returnService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.marketDataRepository = marketDataRepository;
        this.returnService = returnService;
    }

    public Portfolio getPortfolio(Long userId) {
        PortfolioContext context = buildContext(userId);

        BigDecimal availableCash = calculateAvailableCash(context.orders());
        BigDecimal onHoldCash = calculateOnHoldCash(context.orders());
        BigDecimal totalStockShareValue = calculateTotalStockShareValue(context.orders(), context.marketsData());

        BigDecimal totalAccountValue = availableCash.add(onHoldCash).add(totalStockShareValue);
        List<Portfolio.Instrument> instrumentInfoAggregated = aggregateInstrumentInfo(context.orders(), context.marketsData(), context.instruments());

        return new Portfolio("AR$", totalAccountValue, availableCash, onHoldCash,
                totalStockShareValue, instrumentInfoAggregated);
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
                () -> marketDataRepository.findAllById(instrumentIds));
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
    private BigDecimal calculateTotalStockShareValue(List<Order> orders, List<MarketData> marketsData) {
        BigDecimal totalStockShareValue = BigDecimal.ZERO;

        List<Order> ordersToCalculate = orders.stream().filter(order -> order.toShareSwapped() || order.toCashSwapped()).toList();
        Map<Long, MarketData> instrumentToMarketdataMap = getLastInstrumentMarketData(marketsData);

        Map<Long, Long> instrumentToCantMap = calculateInstrumentCantMap(ordersToCalculate);

        for (Map.Entry<Long, Long> instrumentCant : instrumentToCantMap.entrySet()) {
            MarketData instMarketdata = instrumentToMarketdataMap.get(instrumentCant.getKey());
            BigDecimal totalInstrumentValue = instMarketdata.close().multiply(BigDecimal.valueOf(instrumentCant.getValue()));
            totalStockShareValue = totalStockShareValue.add(totalInstrumentValue);
        }

        return totalStockShareValue;
    }

    // CASH: ✅
    private BigDecimal calculateAvailableCash(List<Order> orders) {
        BigDecimal cash = BigDecimal.ZERO;

        List<Order> ordersToCalculate = orders.stream()
                .filter(order -> !order.isShareOnHold() && !order.isCashOnHold())
                .toList();

        for (Order order: ordersToCalculate) {
            BigDecimal valueToOperate = order.orderValue();

            if(order.isCashIn() || order.toCashSwapped()) {
                cash = cash.add(valueToOperate);
                continue;
            }

            cash = cash.subtract(valueToOperate);
        }

        return cash;
    }

    // ON-HOLD-CASH: ✅
    private BigDecimal calculateOnHoldCash(List<Order> orders) {
        return orders.stream()
                .filter(Order::isCashOnHold)
                .map(Order::orderValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

            instrumentInfoAggregated.add(new Portfolio.Instrument(
                    instrumentId,
                    instrument.ticker(),
                    instrument.name(),
                    size,
                    marketData.close().multiply(BigDecimal.valueOf(size)),
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
