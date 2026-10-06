package com.cocos.portfolio_service.order.utils.mappers;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import com.cocos.portfolio_service.order.api.dtos.SubmitOrderRequest;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderMapperTest {
    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void mapsDecimalRequestBudgetToArsWithoutTruncatingFractionalPart() {
        SubmitOrderRequest request = new SubmitOrderRequest(3L, OrderSide.BUY, OrderType.LIMIT,
                2L, BigDecimal.ZERO, new BigDecimal("150000.5"));

        var command = orderMapper.toOrderSubmit(request);

        assertEquals(new BigDecimal("150000.5"), command.budget().value());
    }

    @Test
    void mapsOrderToEntityAndBackWithoutChangingValues() {
        LocalDateTime datetime = LocalDateTime.of(2026, 10, 5, 12, 30);
        Order order = new Order(11L, 7L, 3L, OrderSide.CASH_IN, 500L,
                new BigDecimal("1.00"), OrderType.MARKET, OrderStatus.FILLED, datetime);

        OrderEntity entity = orderMapper.toOrderEntity(order);
        Order mappedBack = orderMapper.toOrderDomain(entity);

        assertEquals(11L, entity.getId());
        assertEquals(500, entity.getSize());
        assertEquals(datetime, entity.getDatetime());
        assertEquals(order, mappedBack);
    }

    @Test
    void whenDomainSizeExceedsEntityColumnRange_thenMappingFailsInsteadOfTruncating() {
        Order oversizedOrder = new Order(null, 7L, 3L, OrderSide.CASH_IN, (long) Integer.MAX_VALUE + 1,
                BigDecimal.ONE, OrderType.MARKET, OrderStatus.FILLED, LocalDateTime.now());

        assertThrows(ArithmeticException.class, () -> orderMapper.toOrderEntity(oversizedOrder));
    }
}
