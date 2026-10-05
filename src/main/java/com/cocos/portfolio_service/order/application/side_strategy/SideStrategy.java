package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;

public interface SideStrategy {
    Order submit(OrderContext context, OrderToSubmit orderToSubmit);
}
