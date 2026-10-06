package com.cocos.portfolio_service.instrument.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Instrument summary")
public record InstrumentResponse(
        @Schema(example = "30") Long id,
        @Schema(example = "GGAL") String ticker,
        @Schema(example = "Grupo Financiero Galicia") String name) {
}
