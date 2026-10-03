package com.cocos.portfolio_service.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
        String currency,
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        BigDecimal stockShareValue,
        List<Instrument> instruments) {

    public record Instrument(
            Long instrumentId,
            String ticker,
            String name,
            long size,
            BigDecimal marketValue,
            BigDecimal totalReturnPercent) {
    }
}
