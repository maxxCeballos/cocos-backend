package com.cocos.portfolio_service.order.api.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Created order")
public record OrderResponse(
        @Schema(example = "101")
        Long id,
        @Schema(example = "1")
        Long userId,
        @Schema(example = "30")
        Long instrumentId,
        @Schema(description = "Order side", example = "BUY")
        String side,
        @Schema(example = "1")
        Long size,
        @Schema(description = "Unit price in ARS", example = "1250.50")
        BigDecimal price,
        @Schema(description = "Order type", example = "MARKET")
        String type,
        @Schema(description = "Order status", example = "FILLED")
        String status,
        @Schema(example = "2026-10-05T12:30:00")
        LocalDateTime datetime) {
}
