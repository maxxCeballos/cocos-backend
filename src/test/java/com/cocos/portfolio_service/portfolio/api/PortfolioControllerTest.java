package com.cocos.portfolio_service.portfolio.api;

import com.cocos.portfolio_service.portfolio.application.IPortfolioService;
import com.cocos.portfolio_service.portfolio.domain.Portfolio;
import com.cocos.portfolio_service.portfolio.utils.mappers.PortfolioMapperImpl;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@Import({GlobalExceptionHandler.class, PortfolioMapperImpl.class})
class PortfolioControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean IPortfolioService portfolioService;

    static Stream<Integer> positionCounts() { return Stream.of(0, 1, 3); }

    @ParameterizedTest
    @MethodSource("positionCounts")
    void getsPortfolioWithEmptySingleAndMultiplePositions(int count) throws Exception {
        var positions = IntStream.range(0, count).mapToObj(i -> new Portfolio.Instrument(
                (long) i + 1, "T" + i, "Instrument " + i, i + 1L,
                new BigDecimal("100.00"), "$100.00", new BigDecimal("1.0000"))).toList();
        var portfolio = new Portfolio("AR$", new BigDecimal("100.00"), "$100.00",
                new BigDecimal("0.00"), "$0.00", new BigDecimal("0.00"), "$0.00",
                new BigDecimal("100.00"), "$100.00", positions);
        when(portfolioService.getPortfolio(7L)).thenReturn(portfolio);

        mockMvc.perform(get("/api/portfolio/users").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("AR$"))
                .andExpect(jsonPath("$.totalAccountValue").value(100.0))
                .andExpect(jsonPath("$.instruments.length()").value(count));
        verify(portfolioService).getPortfolio(7L);
    }

    @Test
    void rejectsMissingOrNonPositiveUserHeader() throws Exception {
        mockMvc.perform(get("/api/portfolio/users")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/portfolio/users").header("X-User-Id", "0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(portfolioService);
    }

    @Test
    void mapsMissingUserTo404AndUnexpectedFailureTo500() throws Exception {
        when(portfolioService.getPortfolio(404L)).thenThrow(new UserNotFoundException(404L));
        mockMvc.perform(get("/api/portfolio/users").header("X-User-Id", "404"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        when(portfolioService.getPortfolio(8L)).thenThrow(new IllegalStateException("failed"));
        mockMvc.perform(get("/api/portfolio/users").header("X-User-Id", "8"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500));
    }
}
