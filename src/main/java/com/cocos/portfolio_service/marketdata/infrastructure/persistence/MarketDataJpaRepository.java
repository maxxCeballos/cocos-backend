package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MarketDataJpaRepository extends JpaRepository<MarketDataEntity, Long> {

    @Query("""
            SELECT m 
            FROM MarketDataEntity m
            WHERE m.instrumentId = :instrumentId
            ORDER BY date DESC LIMIT 1
        """)
    Optional<MarketDataEntity> findLatestByInstrumentId(Long instrumentId);

    @Query("""
            SELECT m
            FROM MarketDataEntity m
            WHERE m.instrumentId IN :instrumentIds
        """)
    List<MarketDataEntity> findAllByInstrumentId(List<Long> instrumentIds);
}
