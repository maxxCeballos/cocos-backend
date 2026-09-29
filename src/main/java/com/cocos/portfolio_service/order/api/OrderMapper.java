package com.cocos.portfolio_service.order.api;

import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderToSubmit toCommand(SubmitOrderRequest request);

    @Mapping(target = "size", source = "quantity")
    OrderResponse toResponse(Order order);
}
