package com.cocos.portfolio_service.portfolio.api;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        String currency,
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        BigDecimal onHoldCash,
        BigDecimal stockShareValue,
        List<InstrumentResponse> instruments) {

    public record InstrumentResponse(
            Long instrumentId,
            String ticker,
            String name,
            long size,
            BigDecimal marketValue,
            BigDecimal totalReturnPercent) {
    }
}
