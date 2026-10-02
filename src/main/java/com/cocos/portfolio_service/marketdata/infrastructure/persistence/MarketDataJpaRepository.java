package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MarketDataJpaRepository extends JpaRepository<MarketDataEntity, Long> {
    Optional<MarketDataEntity> findByInstrumentId(Long instrumentId);

    @Query("SELECT m FROM MarketDataEntity m " +
            "WHERE m.instrumentId IN :instrumentIds " +
            "AND m.date = (SELECT MAX(sub.date) FROM MarketDataEntity sub WHERE sub.instrumentId = m.instrumentId)")
    List<MarketDataEntity> findCurrentMarketDataByInstrumentId(List<Long> instrumentIds);
}
