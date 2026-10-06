package com.cocos.portfolio_service.instrument.domain;

import java.util.List;

public record InstrumentSearchResult(
        List<Instrument> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
