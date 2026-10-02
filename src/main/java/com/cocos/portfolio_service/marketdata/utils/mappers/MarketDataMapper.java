package com.cocos.portfolio_service.marketdata.utils.mappers;

import com.cocos.portfolio_service.marketdata.domain.MarketData;
import com.cocos.portfolio_service.marketdata.infrastructure.persistence.MarketDataEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MarketDataMapper {

    MarketData toMarketDataDomain(MarketDataEntity marketDataEntity);
}
