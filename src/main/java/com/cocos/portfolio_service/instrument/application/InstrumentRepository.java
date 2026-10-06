package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository {
    Page<Instrument> search(String query, Pageable pageable);
    Page<Instrument> findAll(Pageable pageable);
    Optional<Instrument> findById(Long instrumentId);
    List<Instrument> findAllById(List<Long> instrumentIds);
}
