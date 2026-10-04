package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
class OrderService implements IOrderService {
    private final UserRepository userRepository;
    private final Map<String, SubmitStrategy> strategies;

    OrderService(UserRepository userRepository, Map<String, SubmitStrategy> strategies) {
        this.userRepository = userRepository;
        this.strategies = strategies;
    }

    @Override
    @Transactional
    public Order submit(Long userId, OrderToSubmit order) {

        Optional<User> userOpt = userRepository.findById(userId);
        if(userOpt.isEmpty()) throw new UserNotFoundException(userId);

        SubmitStrategy strategy = strategies.get(order.type().toString());

        Order orderSaved = strategy.sumbit(order);

        return orderSaved;
    }
}
