package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;

public interface IInstrument {
    InstrumentSearchResult search(String query, int page, int size);
}
