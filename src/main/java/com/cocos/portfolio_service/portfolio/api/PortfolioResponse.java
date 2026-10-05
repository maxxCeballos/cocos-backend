package com.cocos.portfolio_service.portfolio.api;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        String currency,
        BigDecimal totalAccountValue,
        String totalAccountValueLabel,
        BigDecimal availableCash,
        String availableCashLabel,
        BigDecimal onHoldCash,
        String onHoldCashLabel,
        BigDecimal stockShareValue,
        String stockShareValueLabel,
        List<InstrumentResponse> instruments) {

    public record InstrumentResponse(
            Long instrumentId,
            String ticker,
            String name,
            long size,
            BigDecimal marketValue,
            String marketValueLabel,
            BigDecimal totalReturnPercent) {
    }
}
