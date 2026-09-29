package com.cocos.portfolio_service.order.api;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long userId,
        Long instrumentId,
        String side,
        Long size,
        BigDecimal price,
        String type,
        String status,
        Instant datetime) {
}
