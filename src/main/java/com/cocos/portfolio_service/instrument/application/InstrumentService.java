package com.cocos.portfolio_service.instrument.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchCacheEntry;
import com.cocos.portfolio_service.instrument.domain.InstrumentSearchResult;
import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.instrument.domain.errors.InvalidInstrumentSearchQueryException;
import com.cocos.portfolio_service.shared.infrastructure.cache.SharedListCache;
import com.cocos.portfolio_service.shared.domain.errors.ValkeyUnavailableException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
class InstrumentService implements IInstrument {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentService.class);
    private static final String CACHE_KEY_PREFIX = "instrument:search:";

    private final InstrumentRepository instrumentRepository;
    private final SharedListCache cache;
    private final JsonMapper jsonMapper;

    InstrumentService(InstrumentRepository instrumentRepository, SharedListCache cache, JsonMapper jsonMapper) {
        this.instrumentRepository = instrumentRepository;
        this.cache = cache;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public InstrumentSearchResult search(Long userId, Long instrumentId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (instrumentId != null) {
            return searchById(userId, instrumentId, pageable);
        }

        if (query != null) {
            if (query.isBlank()) {
                throw new InvalidInstrumentSearchQueryException();
            }
            return toSearchResult(instrumentRepository.search(query.trim(), pageable));
        }

        return searchCachedInstruments(userId, pageable);
    }

    private InstrumentSearchResult searchById(Long userId, Long instrumentId, Pageable pageable) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new InstrumentNotFoundException(instrumentId));
        String cacheKey = cacheKey(userId);
        try {
            String value = jsonMapper.writeValueAsString(InstrumentSearchCacheEntry.from(instrument));
            cache.upsertById(cacheKey, instrumentId, value);
        } catch (JacksonException exception) {
            logger.error("Unable to serialize instrument cache entry for key {}", cacheKey, exception);
            throw new ValkeyUnavailableException("Unable to write instrument to Valkey cache", exception);
        }

        List<Instrument> content = pageable.getPageNumber() == 0 ? List.of(instrument) : List.of();
        return new InstrumentSearchResult(content, pageable.getPageNumber(), pageable.getPageSize(), 1, 1);
    }

    private InstrumentSearchResult searchCachedInstruments(Long userId, Pageable pageable) {
        String cacheKey = cacheKey(userId);
        try {
            List<Instrument> instruments = readUniqueCacheEntries(cache.range(cacheKey, 0, -1));
            long totalElements = instruments.size();
            int totalPages = totalElements == 0 ? 0
                    : (int) ((totalElements + pageable.getPageSize() - 1) / pageable.getPageSize());
            int start = (int) Math.min(pageable.getOffset(), totalElements);
            int end = (int) Math.min((long) start + pageable.getPageSize(), totalElements);
            List<Instrument> content = new ArrayList<>(instruments.subList(start, end));
            return new InstrumentSearchResult(content, pageable.getPageNumber(), pageable.getPageSize(),
                    totalElements, totalPages);
        } catch (JacksonException exception) {
            logger.error("Unable to deserialize instrument cache entry for key {}", cacheKey, exception);
            throw new ValkeyUnavailableException("Unable to read instrument from Valkey cache", exception);
        }
    }

    private List<Instrument> readUniqueCacheEntries(List<String> values) throws JacksonException {
        Map<Long, Instrument> instrumentsById = new LinkedHashMap<>();
        for (String value : values) {
            Instrument instrument = jsonMapper.readValue(value, InstrumentSearchCacheEntry.class).toInstrument();
            instrumentsById.putIfAbsent(instrument.id(), instrument);
        }
        return new ArrayList<>(instrumentsById.values());
    }

    private InstrumentSearchResult toSearchResult(org.springframework.data.domain.Page<Instrument> result) {
        return new InstrumentSearchResult(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private String cacheKey(Long userId) {
        return CACHE_KEY_PREFIX + userId;
    }
}
