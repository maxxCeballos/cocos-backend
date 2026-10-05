package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.order.application.ports.OrderRepository;
import com.cocos.portfolio_service.order.application.side_strategy.SideStrategy;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService;
import com.cocos.portfolio_service.shared.infrastructure.lock.SharedLockService.LockHandle;
import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
class OrderService implements IOrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final Map<String, SideStrategy> strategies;
    private final SharedLockService lockService;

    OrderService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            InstrumentRepository instrumentRepository,
            Map<String, SideStrategy> strategies,
            SharedLockService lockService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.instrumentRepository = instrumentRepository;
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
        OrderContext context = new OrderContext(userOpt.get(), instOpt.get());

        LockHandle lock = lockService.acquireForUser(userId);

        try {
            Order orderToSave = strategy.submit(context, orderToSubmit);
            Order orderSaved = orderRepository.save(orderToSave);
            return orderSaved;
        } finally {
            lock.release();
        }
    }
}
