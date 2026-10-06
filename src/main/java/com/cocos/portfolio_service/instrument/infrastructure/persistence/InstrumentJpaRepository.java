package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, Long> {
    Page<InstrumentEntity> findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(
            String ticker, String name, Pageable pageable);
}
