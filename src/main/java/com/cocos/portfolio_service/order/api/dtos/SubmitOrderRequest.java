package com.cocos.portfolio_service.order.api.dtos;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Order submission request", example = "{\"instrumentId\":30,\"side\":\"BUY\",\"type\":\"MARKET\",\"size\":1,\"price\":0,\"budget\":0}")
public record SubmitOrderRequest(
        @Schema(description = "Instrument identifier", example = "30")
        @NotNull @Positive Long instrumentId,
        @Schema(description = "Order side", allowableValues = {"BUY", "SELL", "CASH_IN", "CASH_OUT"}, example = "BUY")
        @NotNull OrderSide side,
        @Schema(description = "Order type", allowableValues = {"MARKET", "LIMIT"}, example = "MARKET")
        @NotNull OrderType type,

        @Schema(description = "Quantity of instruments. For BUY orders, a positive size takes precedence over budget.", example = "1", minimum = "0")
        @Min(value = 0, message = "El size de la orden no puede ser negativo")
        @NotNull
        Long size,

        @Schema(description = "Unit price for LIMIT orders. MARKET orders use the latest market price.", example = "0", minimum = "0")
        @Min(value = 0, message = "El price de la orden no puede ser negativo")
        @NotNull
        BigDecimal price,

        @Schema(description = "Amount in ARS to invest instead of quantity; applies to BUY MARKET and LIMIT orders when size is zero.", example = "0", minimum = "0")
        @Min(value = 0, message = "El budget de la orden no puede ser negativo")
        @NotNull
        BigDecimal budget) {
}
