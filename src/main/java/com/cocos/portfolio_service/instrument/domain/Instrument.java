package com.cocos.portfolio_service.instrument.domain;

import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;

public record Instrument(Long id, String ticker, String name, InstrumentType type) {
}
