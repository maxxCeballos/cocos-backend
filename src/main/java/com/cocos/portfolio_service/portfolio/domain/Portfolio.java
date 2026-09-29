package com.cocos.portfolio_service.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        List<Instrument> instruments) {

    public record Instrument(
            Long instrumentId,
            String ticker,
            String name,
            long quantity,
            BigDecimal marketValue,
            BigDecimal totalReturnPercent) {
    }
}
