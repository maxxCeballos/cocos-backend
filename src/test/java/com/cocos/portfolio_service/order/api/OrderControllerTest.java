package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.instrument.domain.errors.InstrumentNotFoundException;
import com.cocos.portfolio_service.order.application.OrderCommands;
import com.cocos.portfolio_service.shared.api.GlobalExceptionHandler;
import com.cocos.portfolio_service.shared.domain.errors.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCommands orderCommands;

    @Test
    void whenOrderIsValid_thenSubmit_returnsCreatedOrder() throws Exception {
        // ARRANGE
        when(orderCommands.submit(any(SubmitOrderRequest.class))).thenReturn(order(90L, 7L, "FILLED"));

        // ACT
        var result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":7,"instrumentId":2,"side":"BUY","type":"MARKET","quantity":3}
                                """));

        // ASSERT
        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(90))
                .andExpect(jsonPath("$.status").value("FILLED"));

        verify(orderCommands).submit(any(SubmitOrderRequest.class));
    }

    @Test
    void whenRequiredSideIsMissing_thenSubmit_returnsBadRequest() throws Exception {
        // ARRANGE
        var request = post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":7,"instrumentId":2,"type":"LIMIT","quantity":3,"price":100}
                                """);

        // ACT
        var result = mockMvc.perform(request);

        // ASSERT
        result.andExpect(status().isBadRequest());
    }

    @Test
    void whenUserDoesNotExist_thenSubmit_returnsNotFound() throws Exception {
        // ARRANGE
        when(orderCommands.submit(any(SubmitOrderRequest.class))).thenThrow(new UserNotFoundException(404L));

        // ACT
        var result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderRequest()));

        // ASSERT
        result
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with id 404 was not found"));
    }

    @Test
    void whenInstrumentDoesNotExist_thenSubmit_returnsNotFound() throws Exception {
        // ARRANGE
        when(orderCommands.submit(any(SubmitOrderRequest.class))).thenThrow(new InstrumentNotFoundException(404L));

        // ACT
        var result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderRequest()));

        // ASSERT
        result
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Instrument with id 404 was not found"));
    }

    private String validOrderRequest() {
        return """
                {"userId":7,"instrumentId":2,"side":"BUY","type":"MARKET","quantity":3}
                """;
    }

    private OrderResponse order(Long id, Long userId, String status) {
        return new OrderResponse(id, userId, 2L, "BUY", 3L, new BigDecimal("100.00"),
                "MARKET", status, Instant.parse("2026-09-29T12:00:00Z"));
    }
}
