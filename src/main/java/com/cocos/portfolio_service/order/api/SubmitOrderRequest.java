package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubmitOrderRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long instrumentId,
        @NotNull OrderSide side,
        @NotNull OrderType type,
        @Positive Long quantity,
        @Positive BigDecimal amount,
        @Positive BigDecimal price) {
}
