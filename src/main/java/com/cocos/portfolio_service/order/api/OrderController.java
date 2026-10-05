package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.order.api.dtos.OrderResponse;
import com.cocos.portfolio_service.order.api.dtos.SubmitOrderRequest;
import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.utils.mappers.OrderMapper;
import com.cocos.portfolio_service.shared.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order submission")
class OrderController {
    private final IOrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(IOrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an order", description = "Submits an order for a user and instrument. For BUY orders, size takes precedence over budget when size is greater than zero. Budget-based sizing is available for BUY MARKET and LIMIT orders. For LIMIT orders, price is the unit limit price; MARKET orders use the latest market price.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid fields",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "The user, instrument, or instrument market data was not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "The user has another operation in progress",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Valkey is unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public OrderResponse submit(
            @Parameter(description = "Positive ID of the user submitting the order", required = true, example = "1")
            @RequestHeader("X-User-Id") @Positive Long userId,
            @Valid @RequestBody SubmitOrderRequest request
    ) {

        OrderToSubmit orderToSubmit = orderMapper.toOrderSubmit(request);

        return orderMapper.toResponse(orderService.submit(userId, orderToSubmit));
    }
}
