package com.cocos.portfolio_service.order.utils.mappers;

import com.cocos.portfolio_service.order.api.dtos.OrderResponse;
import com.cocos.portfolio_service.order.api.dtos.SubmitOrderRequest;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderToSubmit toCommand(SubmitOrderRequest request);

    OrderResponse toResponse(Order order);

    Order toOrderDomain(OrderEntity order);
}
