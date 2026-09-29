package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.portfolio.api.PortfolioResponse;

public interface PortfolioQuery {
    PortfolioResponse getPortfolio(Long userId);
}
