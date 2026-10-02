package com.cocos.portfolio_service.instrument.utils.mappers;

import com.cocos.portfolio_service.instrument.api.InstrumentResponse;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;
import com.cocos.portfolio_service.instrument.infrastructure.persistence.InstrumentEntity;
import com.cocos.portfolio_service.shared.api.PageResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstrumentMapper {
    InstrumentResponse toResponse(Instrument instrument);

    PageResponse<InstrumentResponse> toPageResponse(InstrumentSearchResult result);

    Instrument toInstrumentDomain(InstrumentEntity instrumentEntity);

}
