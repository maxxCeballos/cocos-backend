package com.cocos.portfolio_service.instrument.domain;

import java.math.BigDecimal;

public record Instrument(Long id, String ticker, String name, String type, BigDecimal close) {
}
