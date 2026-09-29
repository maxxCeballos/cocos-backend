package com.cocos.portfolio_service.instrument.api;

import com.cocos.portfolio_service.instrument.application.InstrumentSearch;
import com.cocos.portfolio_service.shared.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {
    private final InstrumentSearch instrumentSearch;

    public InstrumentController(InstrumentSearch instrumentSearch) {
        this.instrumentSearch = instrumentSearch;
    }

    @GetMapping
    public PageResponse<InstrumentResponse> search(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return instrumentSearch.search(query, page, size);
    }
}
