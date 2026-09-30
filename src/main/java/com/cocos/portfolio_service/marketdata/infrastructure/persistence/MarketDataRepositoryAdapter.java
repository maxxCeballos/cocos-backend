package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MarketDataRepositoryAdapter implements MarketDataRepository {
    private final MarketDataJpaRepository repository;

    public MarketDataRepositoryAdapter(MarketDataJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MarketData> findLatestByInstrumentId(Long instrumentId) {
        return repository.findFirstByInstrumentIdOrderByDateDescIdDesc(Math.toIntExact(instrumentId))
                .map(entity -> new MarketData(entity.getId().longValue(), entity.getInstrumentId().longValue(), entity.getClose(),
                        entity.getPreviousClose(), entity.getDate()));
    }
}
