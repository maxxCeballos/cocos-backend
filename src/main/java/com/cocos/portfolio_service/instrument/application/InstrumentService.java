package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
class InstrumentService implements IInstrument {
    @Override
    public InstrumentSearchResult search(String query, int page, int size) {
        List<Instrument> mockResults = List.of(
                new Instrument(2L, "AAPL", "Apple Inc.", "ACCION", new BigDecimal("30000.00")),
                new Instrument(3L, "MSFT", "Microsoft Corp.", "ACCION", new BigDecimal("40000.00")));

        return new InstrumentSearchResult(mockResults, page, size, mockResults.size(), 1);
    }
}
