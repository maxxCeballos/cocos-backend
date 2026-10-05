package com.cocos.portfolio_service.marketdata.domain;

import java.util.List;
import java.util.Optional;

public interface MarketDataRepository {
    Optional<MarketData> findLatestByInstrumentId(Long instrumentId);
    List<MarketData> findAllByInstrumentId(List<Long> instrumentIds);
}
