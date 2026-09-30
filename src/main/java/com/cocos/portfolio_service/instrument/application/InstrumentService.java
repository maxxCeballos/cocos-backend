package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

@Service
class InstrumentService implements IInstrument {
    private final InstrumentRepository instrumentRepository;

    InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Override
    public InstrumentSearchResult search(String query, int page, int size) {
        var result = instrumentRepository.search(query.trim(), PageRequest.of(page, size));
        return new InstrumentSearchResult(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
