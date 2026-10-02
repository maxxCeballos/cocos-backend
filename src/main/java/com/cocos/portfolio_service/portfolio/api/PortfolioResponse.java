package com.cocos.portfolio_service.portfolio.api;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        String userAccountNumber,
        String currency,
        BigDecimal totalAccountValue,
        BigDecimal availableCash,
        List<InstrumentResponse> instruments) {

    public record InstrumentResponse(
            Long instrumentId,
            String ticker,
            String name,
            long quantity,
            BigDecimal marketValue,
            BigDecimal totalReturnPercent) {
    }
}
