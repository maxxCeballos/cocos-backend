package com.cocos.portfolio_service.portfolio.api;

import com.cocos.portfolio_service.portfolio.application.PortfolioQuery;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    private final PortfolioQuery portfolioQuery;

    public PortfolioController(PortfolioQuery portfolioQuery) {
        this.portfolioQuery = portfolioQuery;
    }

    @GetMapping("/users/{userId}")
    public PortfolioResponse getPortfolio(@PathVariable @Positive Long userId) {
        return portfolioQuery.getPortfolio(userId);
    }
}
