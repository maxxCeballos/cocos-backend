package com.cocos.portfolio_service.marketdata.domain.errors;

public class InvalidMarketDataException extends RuntimeException {
    public InvalidMarketDataException(Long instrumentId) {
        super("Market data for instrument " + instrumentId + " contains an invalid price");
    }
}
