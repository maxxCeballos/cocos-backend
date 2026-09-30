package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface InstrumentRepository {
    Page<Instrument> search(String query, Pageable pageable);
    Optional<Instrument> findById(Long instrumentId);
}
