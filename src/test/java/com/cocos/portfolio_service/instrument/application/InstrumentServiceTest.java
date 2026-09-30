package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {
    @Mock private InstrumentRepository instrumentRepository;
    @InjectMocks private InstrumentService instrumentService;

    @Test
    void whenRepositoryFindsInstruments_thenSearch_returnsPageMetadataAndContent() {
        // ARRANGE
        var instruments = List.of(new Instrument(1L, "GGAL", "Grupo Galicia", "ACCIONES", new BigDecimal("100.00")));
        when(instrumentRepository.search("gal", PageRequest.of(1, 5)))
                .thenReturn(new PageImpl<>(instruments, PageRequest.of(1, 5), 6));

        // ACT
        var result = instrumentService.search(" gal ", 1, 5);

        // ASSERT
        assertEquals(instruments, result.content());
        assertEquals(1, result.page());
        assertEquals(5, result.size());
        assertEquals(6, result.totalElements());
        assertEquals(2, result.totalPages());
        verify(instrumentRepository).search("gal", PageRequest.of(1, 5));
    }

    @Test
    void whenRepositoryFindsNoInstruments_thenSearch_returnsEmptyPage() {
        // ARRANGE
        when(instrumentRepository.search("missing", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        // ACT
        var result = instrumentService.search("missing", 0, 10);

        // ASSERT
        assertEquals(List.of(), result.content());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void whenRepositoryThrows_thenSearch_propagatesFailure() {
        // ARRANGE
        when(instrumentRepository.search("gal", PageRequest.of(0, 10)))
                .thenThrow(new IllegalStateException("database unavailable"));

        // ACT & ASSERT
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> instrumentService.search("gal", 0, 10));
    }
}
