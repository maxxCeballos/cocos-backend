package com.cocos.portfolio_service.order.api.dtos;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubmitOrderRequest(
        @NotNull @Positive Long instrumentId,
        @NotNull OrderSide side,
        @NotNull OrderType type,

        @Min(value = 0, message = "El size de la orden no puede ser negativo")
        @NotNull
        Long size,

        @Min(value = 0, message = "El price de la orden no puede ser negativo")
        @NotNull
        BigDecimal price,

        @Min(value = 0, message = "El budget de la orden no puede ser negativo")
        @NotNull
        BigDecimal budget) {
}
