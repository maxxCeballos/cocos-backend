package com.cocos.portfolio_service.marketdata.domain;

import java.util.Optional;

public interface MarketDataRepository {
    Optional<MarketData> findLatestByInstrumentId(Long instrumentId);
}
