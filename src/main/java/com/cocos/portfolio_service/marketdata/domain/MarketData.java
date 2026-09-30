package com.cocos.portfolio_service.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketData(Long id, Long instrumentId, BigDecimal close, BigDecimal previousClose, LocalDate date) {
}
