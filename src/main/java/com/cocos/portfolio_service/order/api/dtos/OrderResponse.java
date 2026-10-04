package com.cocos.portfolio_service.order.api.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long userId,
        Long instrumentId,
        String side,
        Long size,
        BigDecimal price,
        String type,
        String status,
        LocalDateTime datetime) {
}
