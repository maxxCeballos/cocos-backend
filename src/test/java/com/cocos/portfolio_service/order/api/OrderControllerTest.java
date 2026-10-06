package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.marketdata.domain.errors.MarketDataNotFoundException;
import com.cocos.portfolio_service.order.api.dtos.OrderResponse;
import com.cocos.portfolio_service.order.api.dtos.SubmitOrderRequest;
import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.domain.errors.InvalidOrderException;
import com.cocos.portfolio_service.order.utils.mappers.OrderMapper;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.domain.errors.LockAcquisitionTimeoutException;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import com.cocos.portfolio_service.shared.domain.errors.ValkeyUnavailableException;
import com.cocos.portfolio_service.shared.domain.money.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean IOrderService orderService;
    @MockitoBean OrderMapper orderMapper;

    @ParameterizedTest
    @CsvSource({"BUY,MARKET,FILLED", "BUY,LIMIT,NEW", "BUY,MARKET,REJECTED", "SELL,LIMIT,NEW"})
    void submitsOrdersAndReturnsTheirCurrentStatuses(String side, String type, String orderStatus) throws Exception {
        var response = response(OrderStatus.valueOf(orderStatus), OrderSide.valueOf(side), OrderType.valueOf(type));
        when(orderService.submit(eq(7L), any(OrderToSubmit.class))).thenReturn(
                new Order(response.id(), 7L, 30L, OrderSide.valueOf(side), 1L, new BigDecimal("100"),
                        OrderType.valueOf(type), OrderStatus.valueOf(orderStatus), LocalDateTime.of(2026, 10, 5, 12, 30)));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(response);
        when(orderMapper.toOrderSubmit(any(SubmitOrderRequest.class))).thenAnswer(invocation -> {
            var request = invocation.<SubmitOrderRequest>getArgument(0);
            return new OrderToSubmit(request.instrumentId(), request.side(), request.type(), request.size(),
                    new Money.ARS(request.price()), new Money.ARS(request.budget()));
        });

        mockMvc.perform(post("/api/orders/submit").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON).content(request(side, type)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(orderStatus))
                .andExpect(jsonPath("$.side").value(side))
                .andExpect(jsonPath("$.type").value(type));
        verify(orderService).submit(eq(7L), any(OrderToSubmit.class));
    }

    @Test
    void rejectsMissingInvalidHeadersAndInvalidRequestBodies() throws Exception {
        mockMvc.perform(post("/api/orders/submit").contentType(MediaType.APPLICATION_JSON).content(request("BUY", "MARKET")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders/submit").header("X-User-Id", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(request("BUY", "MARKET")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders/submit").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instrumentId\":30}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @MethodSource("domainErrors")
    void mapsDomainAndInfrastructureErrors(Exception exception, int statusCode) throws Exception {
        when(orderMapper.toOrderSubmit(any(SubmitOrderRequest.class))).thenReturn(
                new OrderToSubmit(30L, OrderSide.BUY, OrderType.MARKET, 1L,
                        new Money.ARS(BigDecimal.ZERO), new Money.ARS(BigDecimal.ZERO)));
        when(orderService.submit(eq(7L), any(OrderToSubmit.class))).thenThrow(exception);
        mockMvc.perform(post("/api/orders/submit").header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON).content(request("BUY", "MARKET")))
                .andExpect(status().is(statusCode)).andExpect(jsonPath("$.status").value(statusCode));
    }

    static Stream<Object[]> domainErrors() {
        return Stream.of(
                new Object[]{new UserNotFoundException(7L), 404},
                new Object[]{new InstrumentNotFoundException(30L), 404},
                new Object[]{new MarketDataNotFoundException(30L), 404},
                new Object[]{new InvalidOrderException("invalid"), 400},
                new Object[]{new LockAcquisitionTimeoutException("lock:user:7"), 409},
                new Object[]{new ValkeyUnavailableException("unavailable"), 503},
                new Object[]{new IllegalStateException("failed"), 500});
    }

    private static String request(String side, String type) {
        return """
                {"instrumentId":30,"side":"%s","type":"%s","size":1,"price":100,"budget":0}
                """.formatted(side, type).trim();
    }

    private static OrderResponse response(OrderStatus status, OrderSide side, OrderType type) {
        return new OrderResponse(90L, 7L, 30L, side.name(), 1L, new BigDecimal("100"), type.name(),
                status.name(), LocalDateTime.of(2026, 10, 5, 12, 30));
    }
}
