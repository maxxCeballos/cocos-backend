package com.cocos.portfolio_service.instrument.api;

import com.cocos.portfolio_service.instrument.application.IInstrument;
import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.api.PageResponse;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.domain.errors.ValkeyUnavailableException;
import com.cocos.portfolio_service.instrument.utils.mappers.InstrumentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstrumentController.class)
@Import(GlobalExceptionHandler.class)
class InstrumentControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean IInstrument instrumentSearch;
    @MockitoBean InstrumentMapper instrumentMapper;

    static Stream<Integer> resultCounts() { return Stream.of(0, 1, 3); }

    @ParameterizedTest
    @MethodSource("resultCounts")
    void searchReturnsEmptySingleAndMultiplePages(int count) throws Exception {
        var instruments = Stream.iterate(1L, id -> id + 1).limit(count)
                .map(id -> new Instrument(id, "T" + id, "Instrument " + id, null)).toList();
        var result = new InstrumentSearchResult(instruments, 0, 20, count, count == 0 ? 0 : 1);
        var response = new PageResponse<>(instruments.stream()
                .map(i -> new InstrumentResponse(i.id(), i.ticker(), i.name())).toList(),
                0, 20, count, count == 0 ? 0 : 1);
        when(instrumentSearch.search(7L, null, null, 0, 20)).thenReturn(result);
        when(instrumentMapper.toPageResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(count))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(count));
        verify(instrumentSearch).search(7L, null, null, 0, 20);
    }

    @Test
    void searchSupportsIdQueryAndPaginationParameters() throws Exception {
        var result = new InstrumentSearchResult(List.of(), 2, 5, 10, 2);
        when(instrumentSearch.search(7L, 30L, "GGAL", 2, 5)).thenReturn(result);
        when(instrumentMapper.toPageResponse(result)).thenReturn(new PageResponse<>(List.of(), 2, 5, 10, 2));

        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "7")
                        .param("instrumentId", "30").param("query", "GGAL")
                        .param("page", "2").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2));
        verify(instrumentSearch).search(7L, 30L, "GGAL", 2, 5);
    }

    @Test
    void searchRejectsMissingOrInvalidHeadersAndInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/instruments/search")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "7").param("size", "101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(instrumentSearch);
    }

    @ParameterizedTest
    @MethodSource("notFoundErrors")
    void searchReturnsNotFoundForMissingUserOrInstrument(RuntimeException exception) throws Exception {
        when(instrumentSearch.search(404L, null, null, 0, 20)).thenThrow(exception);
        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "404"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    static Stream<RuntimeException> notFoundErrors() {
        return Stream.of(new UserNotFoundException(404L), new InstrumentNotFoundException(404L));
    }

    @Test
    void searchMapsUnavailableCacheTo503AndUnexpectedFailuresTo500() throws Exception {
        when(instrumentSearch.search(7L, null, null, 0, 20))
                .thenThrow(new ValkeyUnavailableException("unavailable"));
        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "7"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503));
        when(instrumentSearch.search(8L, null, null, 0, 20))
                .thenThrow(new IllegalStateException("failed"));
        mockMvc.perform(get("/api/instruments/search").header("X-User-Id", "8"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500));
    }
}
