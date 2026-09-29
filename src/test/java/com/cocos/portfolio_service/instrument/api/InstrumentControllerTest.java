package com.cocos.portfolio_service.instrument.api;

import com.cocos.portfolio_service.instrument.application.InstrumentSearch;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.api.PageResponse;
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

@WebMvcTest(InstrumentController.class)
@Import(GlobalExceptionHandler.class)
class InstrumentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstrumentSearch instrumentSearch;

    @Test
    void whenSearchReturnsNoInstruments_thenSearch_returnsEmptyPage() throws Exception {
        // ARRANGE
        when(instrumentSearch.search("missing", 0, 10)).thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));

        // ACT
        var result = mockMvc.perform(get("/api/instruments").param("query", "missing")
                .param("page", "0").param("size", "10"));

        // ASSERT
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(instrumentSearch).search("missing", 0, 10);
    }

    @Test
    void whenSearchReturnsMultipleInstruments_thenSearch_returnsPagedContent() throws Exception {
        // ARRANGE
        when(instrumentSearch.search("tech", 1, 2)).thenReturn(new PageResponse<>(List.of(
                new InstrumentResponse(2L, "AAPL", "Apple Inc.", "ACCION", new BigDecimal("30000.00")),
                new InstrumentResponse(3L, "MSFT", "Microsoft Corp.", "ACCION", new BigDecimal("40000.00"))),
                1, 2, 4, 2));

        // ACT
        var result = mockMvc.perform(get("/api/instruments").param("query", "tech")
                .param("page", "1").param("size", "2"));

        // ASSERT
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.content[1].ticker").value("MSFT"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(instrumentSearch).search("tech", 1, 2);
    }

    @Test
    void whenSearchThrows_thenSearch_returnsInternalServerError() throws Exception {
        // ARRANGE
        when(instrumentSearch.search("tech", 0, 20)).thenThrow(new IllegalStateException("search failed"));

        // ACT
        var result = mockMvc.perform(get("/api/instruments").param("query", "tech"));

        // ASSERT
        result
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(instrumentSearch).search("tech", 0, 20);
    }

}
