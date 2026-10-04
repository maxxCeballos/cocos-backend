package com.cocos.portfolio_service.order.application.ports;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;

public interface IOrderService {
    Order submit(Long userId, OrderToSubmit command);
}
