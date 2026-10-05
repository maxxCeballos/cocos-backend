package com.cocos.portfolio_service.portfolio.api;

import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Portfolio balances and positions")
public record PortfolioResponse(
        @Schema(description = "Portfolio currency", example = "AR$")
        String currency,
        @Schema(description = "Total account value", example = "150000.00")
        BigDecimal totalAccountValue,
        @Schema(description = "Formatted total account value", example = "$150000.00")
        String totalAccountValueLabel,
        @Schema(description = "Available cash", example = "25000.00")
        BigDecimal availableCash,
        @Schema(description = "Formatted available cash", example = "$25000.00")
        String availableCashLabel,
        @Schema(description = "Cash held for open orders", example = "5000.00")
        BigDecimal onHoldCash,
        @Schema(description = "Formatted cash held for open orders", example = "$5000.00")
        String onHoldCashLabel,
        @Schema(description = "Market value of stock holdings", example = "120000.00")
        BigDecimal stockShareValue,
        @Schema(description = "Formatted market value of stock holdings", example = "$120000.00")
        String stockShareValueLabel,
        @Schema(description = "Current positions")
        List<InstrumentResponse> instruments) {

    @Schema(description = "Portfolio instrument position")
    public record InstrumentResponse(
            @Schema(example = "30")
            Long instrumentId,
            @Schema(example = "GGAL")
            String ticker,
            @Schema(example = "Grupo Financiero Galicia")
            String name,
            @Schema(description = "Current position quantity", example = "10")
            long size,
            @Schema(description = "Current market value", example = "12500.00")
            BigDecimal marketValue,
            @Schema(description = "Formatted current market value", example = "$12500.00")
            String marketValueLabel,
            @Schema(description = "Total return percentage", example = "4.2500")
            BigDecimal totalReturnPercent) {
    }
}
