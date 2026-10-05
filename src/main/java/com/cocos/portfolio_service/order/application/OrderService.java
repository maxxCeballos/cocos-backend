package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.order.application.side_strategy.SideStrategy;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
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
    private final Map<String, SideStrategy> strategies;
    private final SharedLockService lockService;

    OrderService(UserRepository userRepository, Map<String, SideStrategy> strategies, SharedLockService lockService) {
        this.userRepository = userRepository;
        this.strategies = strategies;
        this.lockService = lockService;
    }

    @Override
    @Transactional
    public Order submit(Long userId, OrderToSubmit order) {

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) throw new UserNotFoundException(userId);

        SideStrategy strategy = strategies.get(order.side().toString());
        LockHandle lock = lockService.acquireForUser(userId);

        try {
            return strategy.submit(userId, order);
        } finally {
            lock.release();
        }
    }
}
