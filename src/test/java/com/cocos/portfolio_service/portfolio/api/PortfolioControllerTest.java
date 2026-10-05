package com.cocos.portfolio_service.portfolio.api;

import com.cocos.portfolio_service.portfolio.application.IPortfolioService;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import com.cocos.portfolio_service.portfolio.utils.mappers.PortfolioMapperImpl;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
@Import({GlobalExceptionHandler.class, PortfolioMapperImpl.class})
class PortfolioControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPortfolioService portfolioQuery;

    @Test
    void whenUserExists_thenGetPortfolio_returnsPortfolio() throws Exception {
        // ARRANGE
        var portfolio = new Portfolio("AR$", new BigDecimal("12500.00"), "$12500.00",
                new BigDecimal("2500.00"), "$2500.00", new BigDecimal("500.00"), "$500.00",
                new BigDecimal("10000.00"), "$10000.00", List.of(
                new Portfolio.Instrument(3L, "ABC", "Example Corp", 10,
                        new BigDecimal("10000.00"), "$10000.00", new BigDecimal("4.25"))));
        when(portfolioQuery.getPortfolio(7L)).thenReturn(portfolio);

        // ACT
        var result = mockMvc.perform(get("/api/portfolio/users/7"));

        // ASSERT
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("AR$"))
                .andExpect(jsonPath("$.totalAccountValue").value(12500.00))
                .andExpect(jsonPath("$.totalAccountValueLabel").value("$12500.00"))
                .andExpect(jsonPath("$.availableCash").value(2500.00))
                .andExpect(jsonPath("$.availableCashLabel").value("$2500.00"))
                .andExpect(jsonPath("$.onHoldCash").value(500.00))
                .andExpect(jsonPath("$.onHoldCashLabel").value("$500.00"))
                .andExpect(jsonPath("$.stockShareValue").value(10000.00))
                .andExpect(jsonPath("$.stockShareValueLabel").value("$10000.00"))
                .andExpect(jsonPath("$.instruments[0].marketValue").value(10000.00))
                .andExpect(jsonPath("$.instruments[0].marketValueLabel").value("$10000.00"))
                .andExpect(jsonPath("$.instruments[0].ticker").value("ABC"))
                .andExpect(jsonPath("$.instruments[0].size").value(10));

        verify(portfolioQuery).getPortfolio(7L);
    }

    @Test
    void whenUserDoesNotExist_thenGetPortfolio_returnsNotFound() throws Exception {
        // ARRANGE
        when(portfolioQuery.getPortfolio(404L)).thenThrow(new UserNotFoundException(404L));

        // ACT
        var result = mockMvc.perform(get("/api/portfolio/users/404"));

        // ASSERT
        result
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 404 was not found"))
                .andExpect(jsonPath("$.path").value("/api/portfolio/users/404"));

        verify(portfolioQuery).getPortfolio(404L);
    }

    @Test
    void whenPortfolioQueryThrows_thenGetPortfolio_returnsInternalServerError() throws Exception {
        // ARRANGE
        when(portfolioQuery.getPortfolio(7L)).thenThrow(new IllegalStateException("database unavailable"));

        // ACT
        var result = mockMvc.perform(get("/api/portfolio/users/7"));

        // ASSERT
        result
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(portfolioQuery).getPortfolio(7L);
    }
}
