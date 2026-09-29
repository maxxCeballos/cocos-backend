package com.cocos.portfolio_service.portfolio.api;

import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {

    PortfolioResponse toResponse(Portfolio portfolio);
}
