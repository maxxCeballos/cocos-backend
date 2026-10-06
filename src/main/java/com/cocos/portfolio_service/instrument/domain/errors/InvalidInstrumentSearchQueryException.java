package com.cocos.portfolio_service.instrument.domain.errors;

public class InvalidInstrumentSearchQueryException extends RuntimeException {
    public InvalidInstrumentSearchQueryException() {
        super("Query must not be blank");
    }
}
