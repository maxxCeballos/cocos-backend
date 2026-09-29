package com.cocos.portfolio_service.instrument.api;

import java.math.BigDecimal;

public record InstrumentResponse(Long id, String ticker, String name, String type, BigDecimal close) {
}
