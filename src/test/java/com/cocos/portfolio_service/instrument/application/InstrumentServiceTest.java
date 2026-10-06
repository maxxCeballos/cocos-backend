package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchCacheEntry;
import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.infrastructure.cache.SharedListCache;
import com.cocos.portfolio_service.user.domain.User;
import com.cocos.portfolio_service.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {
    private static final String CACHE_KEY = "instrument:search:7";

    @Mock private InstrumentRepository instrumentRepository;
    @Mock private UserRepository userRepository;
    @Mock private SharedListCache cache;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private InstrumentService instrumentService;

    @BeforeEach
    void setUp() {
        instrumentService = new InstrumentService(userRepository, instrumentRepository, cache, jsonMapper);
        lenient().when(userRepository.findById(7L)).thenReturn(Optional.of(new User(7L, "test@example.com", "7")));
    }

    @Test
    void whenRepositoryFindsInstruments_thenSearch_returnsPageMetadataAndContent() {
        var instruments = List.of(new Instrument(1L, "GGAL", "Grupo Galicia", InstrumentType.ACCIONES));
        when(instrumentRepository.search("gal", PageRequest.of(1, 5)))
                .thenReturn(new PageImpl<>(instruments, PageRequest.of(1, 5), 6));

        var result = instrumentService.search(7L, null, " gal ", 1, 5);

        assertEquals(instruments, result.content());
        assertEquals(1, result.page());
        assertEquals(5, result.size());
        assertEquals(6, result.totalElements());
        assertEquals(2, result.totalPages());
        verify(instrumentRepository).search("gal", PageRequest.of(1, 5));
    }

    @Test
    void whenRepositoryFindsNoInstruments_thenSearch_returnsEmptyPage() {
        when(instrumentRepository.search("missing", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        var result = instrumentService.search(7L, null, "missing", 0, 10);

        assertEquals(List.of(), result.content());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void whenRepositoryThrows_thenSearch_propagatesFailure() {
        when(instrumentRepository.search("gal", PageRequest.of(0, 10)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class,
                () -> instrumentService.search(7L, null, "gal", 0, 10));
    }

    @Test
    void whenSameInstrumentIsSearchedTwice_thenCacheUsesAtomicUpsertById() throws Exception {
        var first = new Instrument(27L, "GGAL", "Grupo Galicia", InstrumentType.ACCIONES);
        var refreshed = new Instrument(27L, "GGAL", "Grupo Galicia actualizado", InstrumentType.ACCIONES);
        when(instrumentRepository.findById(27L)).thenReturn(Optional.of(first), Optional.of(refreshed));

        instrumentService.search(7L, 27L, null, 0, 20);
        instrumentService.search(7L, 27L, null, 0, 20);

        ArgumentCaptor<String> cacheValues = ArgumentCaptor.forClass(String.class);
        verify(cache, org.mockito.Mockito.times(2)).upsertById(eq(CACHE_KEY), eq(27L), cacheValues.capture());
        assertEquals(new InstrumentSearchCacheEntry(27L, "GGAL", "Grupo Galicia"),
                jsonMapper.readValue(cacheValues.getAllValues().getFirst(), InstrumentSearchCacheEntry.class));
        assertEquals(new InstrumentSearchCacheEntry(27L, "GGAL", "Grupo Galicia actualizado"),
                jsonMapper.readValue(cacheValues.getAllValues().get(1), InstrumentSearchCacheEntry.class));
    }

    @Test
    void whenCacheContainsLegacyDuplicates_thenSearchDeduplicatesBeforePaginating() throws Exception {
        when(cache.range(CACHE_KEY, 0, -1)).thenReturn(List.of(
                cacheEntry(27L, "GGAL", "Grupo Galicia"),
                cacheEntry(27L, "GGAL", "Duplicado antiguo"),
                cacheEntry(28L, "YPF", "YPF"),
                cacheEntry(29L, "PAMP", "Pampa Energía")));

        var result = instrumentService.search(7L, null, null, 1, 2);

        assertEquals(List.of(new Instrument(29L, "PAMP", "Pampa Energía", null)), result.content());
        assertEquals(1, result.page());
        assertEquals(2, result.size());
        assertEquals(3, result.totalElements());
        assertEquals(2, result.totalPages());
        verify(cache).range(CACHE_KEY, 0, -1);
    }

    @Test
    void whenCacheContainsDuplicates_thenSearchPreservesFirstOccurrenceOrder() throws Exception {
        when(cache.range(CACHE_KEY, 0, -1)).thenReturn(List.of(
                cacheEntry(27L, "GGAL", "Grupo Galicia"),
                cacheEntry(28L, "YPF", "YPF"),
                cacheEntry(27L, "GGAL", "Duplicado antiguo")));

        var result = instrumentService.search(7L, null, null, 0, 10);

        assertEquals(List.of(
                new Instrument(27L, "GGAL", "Grupo Galicia", null),
                new Instrument(28L, "YPF", "YPF", null)), result.content());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void whenCacheHasNoDuplicates_thenSearchReturnsAllInOrder() throws Exception {
        when(cache.range(CACHE_KEY, 0, -1)).thenReturn(List.of(
                cacheEntry(27L, "GGAL", "Grupo Galicia"),
                cacheEntry(28L, "YPF", "YPF")));

        var result = instrumentService.search(7L, null, null, 0, 10);

        assertEquals(List.of(
                new Instrument(27L, "GGAL", "Grupo Galicia", null),
                new Instrument(28L, "YPF", "YPF", null)), result.content());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void whenRequestedCachePageIsEmpty_thenMetadataUsesUniqueInstrumentCount() throws Exception {
        when(cache.range(CACHE_KEY, 0, -1)).thenReturn(List.of(
                cacheEntry(27L, "GGAL", "Grupo Galicia"),
                cacheEntry(27L, "GGAL", "Duplicado antiguo")));

        var result = instrumentService.search(7L, null, null, 1, 1);

        assertEquals(List.of(), result.content());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    private String cacheEntry(Long id, String ticker, String name) throws Exception {
        return jsonMapper.writeValueAsString(new InstrumentSearchCacheEntry(id, ticker, name));
    }

    @Test
    void whenUserDoesNotExist_thenSearchFailsBeforeAccessingInstrumentRepositories() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> instrumentService.search(404L, null, null, 0, 20));
        org.mockito.Mockito.verifyNoInteractions(instrumentRepository, cache);
    }
}
