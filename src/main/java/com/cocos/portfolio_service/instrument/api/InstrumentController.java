package com.cocos.portfolio_service.instrument.api;

import com.cocos.portfolio_service.instrument.application.IInstrument;
import com.cocos.portfolio_service.instrument.utils.mappers.InstrumentMapper;
import com.cocos.portfolio_service.shared.api.PageResponse;
import com.cocos.portfolio_service.shared.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/instruments")
class InstrumentController {
    private final IInstrument instrumentSearch;
    private final InstrumentMapper instrumentMapper;

    public InstrumentController(IInstrument instrumentSearch, InstrumentMapper instrumentMapper) {
        this.instrumentSearch = instrumentSearch;
        this.instrumentMapper = instrumentMapper;
    }

    @GetMapping("/search")
    @Operation(summary = "Search instruments", description = "Search by instrument ID or query, or omit both to return instruments cached for this user. Results are paginated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned successfully"),
            @ApiResponse(responseCode = "400", description = "User and instrument IDs must be positive; query must not be blank; page must be non-negative; size must be between 1 and 100",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The user or instrument was not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Valkey is unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PageResponse<InstrumentResponse> search(
            @Parameter(description = "Positive ID of the user whose instrument cache is used", required = true, example = "1")
            @RequestHeader("X-User-Id") @Positive Long userId,
            @Parameter(description = "Return only this instrument when provided", example = "30")
            @RequestParam(required = false) @Positive Long instrumentId,
            @Parameter(description = "Search by ticker or instrument name; must not be blank when provided", example = "GGAL")
            @RequestParam(required = false) String query,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Number of results per page (1–100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return instrumentMapper.toPageResponse(instrumentSearch.search(userId, instrumentId, query, page, size));
    }
}
