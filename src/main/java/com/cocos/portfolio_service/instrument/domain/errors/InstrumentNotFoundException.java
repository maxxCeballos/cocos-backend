package com.cocos.portfolio_service.instrument.domain.errors;

public class InstrumentNotFoundException extends RuntimeException {
    public InstrumentNotFoundException(Long instrumentId) {
        super("Instrument with id " + instrumentId + " was not found");
    }
}
