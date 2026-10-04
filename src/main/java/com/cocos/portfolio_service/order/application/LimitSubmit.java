package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import org.springframework.stereotype.Component;

@Component("LIMIT")
public class LimitSubmit implements SubmitStrategy{

    public Order sumbit(OrderToSubmit orderToSubmit) {
        System.out.printf("ORDEN EXECUTADA POR LIMIT");
        return null;
    }
}
