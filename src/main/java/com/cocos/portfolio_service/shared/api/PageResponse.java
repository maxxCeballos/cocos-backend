package com.cocos.portfolio_service.shared.api;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated result")
public record PageResponse<T>(
        @Schema(description = "Items in this page")
        List<T> content,
        @Schema(description = "Zero-based page number", example = "0")
        int page,
        @Schema(description = "Requested page size", example = "20")
        int size,
        @Schema(description = "Total number of matching items", example = "42")
        long totalElements,
        @Schema(description = "Total number of pages", example = "3")
        int totalPages) {
}
