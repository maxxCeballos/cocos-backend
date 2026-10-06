package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.portfolio.domain.Portfolio;

public interface IPortfolioService {
    Portfolio getPortfolio(Long userId);
}
