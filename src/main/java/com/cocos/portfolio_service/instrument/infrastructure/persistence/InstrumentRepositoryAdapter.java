package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import com.cocos.portfolio_service.instrument.application.InstrumentRepository;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.utils.mappers.InstrumentMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InstrumentRepositoryAdapter implements InstrumentRepository {
    private final InstrumentJpaRepository instruments;
    private final InstrumentMapper instrumentMapper;

    public InstrumentRepositoryAdapter(InstrumentJpaRepository instruments, InstrumentMapper instrumentMapper) {
        this.instruments = instruments;
        this.instrumentMapper = instrumentMapper;
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

    public List<Instrument> findAllById(List<Long> instrumentIds) {
        List<InstrumentEntity> instrumentsDB = instruments.findAllById(instrumentIds);
        return instrumentsDB.stream().map(instrumentMapper::toInstrumentDomain).toList();
    }

    private Instrument toDomain(InstrumentEntity entity) {
        return new Instrument(entity.getId(), entity.getTicker(), entity.getName(), entity.getType());
    }
}
