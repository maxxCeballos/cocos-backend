package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;

public interface SubmitStrategy {
    Order sumbit(OrderToSubmit orderToSubmit);
}
