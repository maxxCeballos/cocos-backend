package com.cocos.portfolio_service.portfolio.application;

import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
class PortfolioService implements IPortfolioService {

    @Override
    public Portfolio getPortfolio(Long userId) {

        return new Portfolio(
                new BigDecimal("12500.00"),
                new BigDecimal("2500.00"),
                List.of(new Portfolio.Instrument(
                        3L,
                        "ABC",
                        "Example Corp",
                        10L,
                        new BigDecimal("10000.00"),
                        new BigDecimal("4.25"))));
    }
}
