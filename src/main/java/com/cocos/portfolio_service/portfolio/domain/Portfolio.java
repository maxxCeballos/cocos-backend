package com.cocos.portfolio_service.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

public record Portfolio(
        String currency,
        BigDecimal totalAccountValue,
        String totalAccountValueLabel,
        BigDecimal availableCash,
        String availableCashLabel,
        BigDecimal onHoldCash,
        String onHoldCashLabel,
        BigDecimal stockShareValue,
        String stockShareValueLabel,
        List<Instrument> instruments) {

    public record Instrument(
            Long instrumentId,
            String ticker,
            String name,
            long size,
            BigDecimal marketValue,
            String marketValueLabel,
            BigDecimal totalReturnPercent) {
    }
}
