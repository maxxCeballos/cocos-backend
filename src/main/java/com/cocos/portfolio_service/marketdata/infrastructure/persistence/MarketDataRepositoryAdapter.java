package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.domain.MarketDataRepository;
import com.cocos.portfolio_service.marketdata.utils.mappers.MarketDataMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MarketDataRepositoryAdapter implements MarketDataRepository {
    private final MarketDataJpaRepository repository;
    private final MarketDataMapper marketDataMapper;

    public MarketDataRepositoryAdapter(MarketDataJpaRepository repository, MarketDataMapper marketDataMapper) {
        this.repository = repository;
        this.marketDataMapper = marketDataMapper;
    }

    @Override
    public Optional<MarketData> findLatestByInstrumentId(Long instrumentId) {
        return repository.findLatestByInstrumentId(instrumentId)
                .map(entity -> new MarketData(entity.getId(), entity.getInstrumentId(), entity.getClose(),
                        entity.getPreviousClose(), entity.getDate()));
    }

    public List<MarketData> findAllByInstrumentId(List<Long> instrumentIds) {
        List<MarketDataEntity> marketDataDB = repository.findAllByInstrumentId(instrumentIds);
        return marketDataDB.stream().map(marketDataMapper::toMarketDataDomain).toList();
    }
}
