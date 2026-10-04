package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import lombok.Setter;

@Setter
public class ExecuteOrder {
    private OrderToSubmit order;
    private SubmitStrategy strategy;

    public ExecuteOrder() {}

    public void execute() {
        this.strategy.sumbit(this.order);
    }
}
