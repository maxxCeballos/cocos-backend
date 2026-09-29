package com.cocos.portfolio_service.instrument.api;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;
import com.cocos.portfolio_service.shared.api.PageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstrumentMapper {
    InstrumentResponse toResponse(Instrument instrument);

    PageResponse<InstrumentResponse> toPageResponse(InstrumentSearchResult result);
}
