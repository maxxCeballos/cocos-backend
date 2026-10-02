package com.cocos.portfolio_service.portfolio.utils.mappers;

import com.cocos.portfolio_service.portfolio.api.PortfolioResponse;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {

    PortfolioResponse toResponse(Portfolio portfolio);
}
