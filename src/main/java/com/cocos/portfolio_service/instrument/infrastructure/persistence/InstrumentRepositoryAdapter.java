package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InstrumentRepositoryAdapter implements InstrumentRepository {
    private final InstrumentJpaRepository instruments;
    private final MarketDataRepository marketData;

    public InstrumentRepositoryAdapter(InstrumentJpaRepository instruments, MarketDataRepository marketData) {
        this.instruments = instruments;
        this.marketData = marketData;
    }

    @Override
    public Page<Instrument> search(String query, Pageable pageable) {
        return instruments.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(query, query, pageable)
                .map(this::toDomain);
    }

    @Override
    public Optional<Instrument> findById(Long instrumentId) {
        return instruments.findById(instrumentId).map(this::toDomain);
    }

    private Instrument toDomain(InstrumentEntity entity) {
        var close = marketData.findLatestByInstrumentId(entity.getId()).map(md -> md.close()).orElse(null);
        return new Instrument(entity.getId(), entity.getTicker(), entity.getName(), entity.getType(), close);
    }
}
