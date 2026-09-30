package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketDataJpaRepository extends JpaRepository<MarketDataEntity, Long> {
    Optional<MarketDataEntity> findFirstByInstrumentIdOrderByDateDescIdDesc(Long instrumentId);
}
