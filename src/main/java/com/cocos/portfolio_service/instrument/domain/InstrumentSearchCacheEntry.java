package com.cocos.portfolio_service.instrument.domain;

public record InstrumentSearchCacheEntry(Long id, String ticker, String name) {
    public Instrument toInstrument() {
        return new Instrument(id, ticker, name, null);
    }

    public static InstrumentSearchCacheEntry from(Instrument instrument) {
        return new InstrumentSearchCacheEntry(instrument.id(), instrument.ticker(), instrument.name());
    }
}
