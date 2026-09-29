package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.api.OrderResponse;
import com.cocos.portfolio_service.order.api.SubmitOrderRequest;

public interface OrderCommands {
    OrderResponse submit(SubmitOrderRequest request);
}
