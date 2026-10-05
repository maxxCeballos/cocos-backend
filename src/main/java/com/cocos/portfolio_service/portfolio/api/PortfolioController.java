package com.cocos.portfolio_service.portfolio.api;

import com.cocos.portfolio_service.portfolio.application.IPortfolioService;
import com.cocos.portfolio_service.portfolio.utils.mappers.PortfolioMapper;
import com.cocos.portfolio_service.shared.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio", description = "Portfolio queries")
class PortfolioController {
    private final IPortfolioService portfolioService;
    private final PortfolioMapper portfolioMapper;

    public PortfolioController(IPortfolioService portfolioQuery, PortfolioMapper portfolioMapper) {
        this.portfolioService = portfolioQuery;
        this.portfolioMapper = portfolioMapper;
    }

    @GetMapping("/users")
    @Operation(summary = "Get user portfolio", description = "Returns the account values and instrument positions for the user identified by X-User-Id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio returned successfully",
                    content = @Content(schema = @Schema(implementation = PortfolioResponse.class))),
            @ApiResponse(responseCode = "400", description = "The user ID must be a positive integer",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The user was not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PortfolioResponse getPortfolio(
            @Parameter(description = "Positive ID of the user", required = true, example = "1")
            @RequestHeader("X-User-Id") @Positive Long userId) {
        return portfolioMapper.toResponse(portfolioService.getPortfolio(userId));
    }
}
