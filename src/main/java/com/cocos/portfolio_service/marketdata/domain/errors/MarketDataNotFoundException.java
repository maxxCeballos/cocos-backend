package com.cocos.portfolio_service.marketdata.domain.errors;

public class MarketDataNotFoundException extends RuntimeException {
    public MarketDataNotFoundException(Long instrumentId) {
        super("Market data for instrument " + instrumentId + " was not found");
    }
}
