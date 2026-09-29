package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.order.application.OrderCommands;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderCommands orderCommands;

    public OrderController(OrderCommands orderCommands) {
        this.orderCommands = orderCommands;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse submit(@Valid @RequestBody SubmitOrderRequest request) {
        return orderCommands.submit(request);
    }
}
