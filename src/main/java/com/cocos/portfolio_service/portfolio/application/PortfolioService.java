package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.order.application.OrderRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class PortfolioService implements IPortfolioService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final MarketDataRepository marketDataRepository;

    PortfolioService(UserRepository userRepository, OrderRepository orderRepository,
                     InstrumentRepository instrumentRepository, MarketDataRepository marketDataRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
        this.marketDataRepository = marketDataRepository;
    }

    public Portfolio getPortfolio(Long userId) {
        // TODO: make this repositories call concurrent
        Optional<User> userOpt = userRepository.findById(userId);
        List<Order> orders = orderRepository.findByUserId(userId);

        if(userOpt.isEmpty()) throw new UserNotFoundException(userId);
        List<Long> instrumentIds = orders.stream().map(Order::instrumentId).toList();

        // TODO: make this repositories call concurrent
        List<Instrument> instruments = instrumentRepository.findAllById(instrumentIds);
        List<MarketData> marketsData = marketDataRepository.findAllById(instrumentIds);

        // TODO: make this methods calls concurrent
        BigDecimal availableCash = calculateAvailableCash(orders);
        BigDecimal stockMarketValue = calculateOrdersFilledValue(orders, marketsData);
        BigDecimal ordersPendingValue = calculateOrdersPendingValue(orders, marketsData);
        BigDecimal totalAccountValue = availableCash.add(stockMarketValue).add(ordersPendingValue);
        List<Portfolio.Instrument> instrumentInfoAggregated = aggregateInstrumentInfo(orders, instruments);

        return new Portfolio(userOpt.get().accountNumber(), "AR$", totalAccountValue, availableCash, instrumentInfoAggregated);
    }

    private BigDecimal calculateOrdersPendingValue(List<Order> orders, List<MarketData> marketsData) {
        BigDecimal ordersPendigValue = BigDecimal.ZERO;

        List<Order> ordersToEvaluate = orders.stream().filter(order -> order.status().equals(OrderStatus.NEW) && !List.of(OrderSide.CASH_IN, OrderSide.CASH_OUT).contains(order.side())).toList();
        Map<Long, MarketData> instrumentMarketdataMap = marketsData.stream().collect(Collectors.toMap(MarketData::instrumentId, Function.identity()));

        for(Order order: ordersToEvaluate) {
            if(order.side() == OrderSide.BUY) {
                BigDecimal totalOrder = order.price().multiply(BigDecimal.valueOf(order.size()));
                ordersPendigValue = ordersPendigValue.add(totalOrder);
            }

            if(order.side() == OrderSide.SELL) {
                MarketData instMarketdata = instrumentMarketdataMap.get(order.instrumentId());
                BigDecimal totalOrder = instMarketdata.close().multiply(BigDecimal.valueOf(order.size()));
                ordersPendigValue = ordersPendigValue.add(totalOrder);
            }
        }

        return ordersPendigValue;
    }

    private BigDecimal calculateOrdersFilledValue(List<Order> orders, List<MarketData> marketsData) {
        BigDecimal ordersFilledValue = BigDecimal.ZERO;

        List<Order> ordersToEvaluate = orders.stream().filter(order -> order.status().equals(OrderStatus.FILLED) && !List.of(OrderSide.CASH_IN, OrderSide.CASH_OUT).contains(order.side())).toList();
        Map<Long, MarketData> instrumentMarketdataMap = marketsData.stream().collect(Collectors.toMap(MarketData::instrumentId, Function.identity()));

        Map<Long, Long> instrumentCantMap = new HashMap<>();

        for (Order order: ordersToEvaluate) {
            if(!instrumentCantMap.containsKey(order.instrumentId())) {
                Long firstCant = order.side() == OrderSide.BUY ? order.size() : -order.size();
                instrumentCantMap.put(order.instrumentId(), firstCant);
            } else {
                Long valueToModify = instrumentCantMap.get(order.instrumentId());
                valueToModify = order.side() == OrderSide.BUY ? (valueToModify + order.size()) : (valueToModify - order.size());
                instrumentCantMap.put(order.instrumentId(), valueToModify);
            }
        }

        for (Map.Entry<Long, Long> entry : instrumentCantMap.entrySet()) {
            MarketData instMarketdata = instrumentMarketdataMap.get(entry.getKey());
            BigDecimal totalOrder = instMarketdata.close().multiply(BigDecimal.valueOf(entry.getValue()));
            ordersFilledValue = ordersFilledValue.add(totalOrder);
        }

        return ordersFilledValue;
    }

    private BigDecimal calculateAvailableCash(List<Order> orders) {
        BigDecimal cash = BigDecimal.ZERO;

        List<Order> executedOrders = orders.stream().filter(
                order -> !List.of(OrderStatus.REJECTED, OrderStatus.CANCELLED, OrderStatus.NEW).contains(order.status())
        ).toList();

        for (Order order: executedOrders) {
            BigDecimal valueToOperate = order.price().multiply(BigDecimal.valueOf(order.size()));

            if(order.side() == OrderSide.CASH_IN || order.side() == OrderSide.SELL) {
                cash = cash.add(valueToOperate);
                continue;
            }

            cash = cash.subtract(valueToOperate);
        }

        return cash;
    }

    private List<Portfolio.Instrument> aggregateInstrumentInfo(List<Order> orders, List<Instrument> instruments) {
        // TODO: aca hay que trabajar un poco mas.
        // Los instrumentos hay que agruparlos por id y ver la forma de ir haciendo un histograma a medida que fue comprando y vendiendo cuanto hizo de rendimiento por dia.

        ArrayList<Portfolio.Instrument> instrumentInfoAggregated = new ArrayList<>();

        List<Order> executedOrders = orders.stream().filter(order -> order.status() == OrderStatus.FILLED ).toList();
        List<Instrument> instrumentsToCalculate = instruments.stream().filter(inst -> !Objects.equals(inst.type(), "MONEDA")).toList(); // TODO: make MONEDA like enum

        Map<Long, Instrument> instrumentMap = instrumentsToCalculate.stream().collect(Collectors.toMap(Instrument::id, Function.identity()));

        for (Order order: executedOrders) {
            Instrument instrument = instrumentMap.get(order.instrumentId());
            if(instrument == null) continue;

            instrumentInfoAggregated.add(
                new Portfolio.Instrument(
                    instrument.id(),
                    instrument.ticker(),
                    instrument.name(),
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
                )
            );
        }

        return instrumentInfoAggregated;
    }
}
