package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.api.InstrumentResponse;
import com.cocos.portfolio_service.shared.api.PageResponse;

public interface InstrumentSearch {
    PageResponse<InstrumentResponse> search(String query, int page, int size);
}
