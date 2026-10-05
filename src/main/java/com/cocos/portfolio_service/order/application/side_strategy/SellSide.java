package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import org.springframework.stereotype.Service;

@Service("SELL")
public class SellSide implements SideStrategy {

    public Order submit(Long userId, OrderToSubmit orderToSubmit) {
        System.out.printf("ORDEN EXECUTADA POR SELL");
        return null;
    }
}
