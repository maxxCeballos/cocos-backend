package com.cocos.portfolio_service.order.application.side_strategy;

import com.cocos.portfolio_service.order.application.OrderContext;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import org.springframework.stereotype.Service;

@Service("CASH_IN")
public class CashInSide implements SideStrategy {

    public Order submit(OrderContext context, OrderToSubmit orderToSubmit) {
        System.out.printf("ORDEN EXECUTADA POR CASH_IN");

        return null;
    }
}
