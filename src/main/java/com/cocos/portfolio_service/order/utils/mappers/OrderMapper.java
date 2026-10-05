package com.cocos.portfolio_service.order.utils.mappers;

import com.cocos.portfolio_service.order.api.dtos.OrderResponse;
import com.cocos.portfolio_service.order.api.dtos.SubmitOrderRequest;
import com.cocos.portfolio_service.order.domain.Order;
import com.cocos.portfolio_service.order.domain.OrderToSubmit;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderEntity;
import com.cocos.portfolio_service.shared.domain.money.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "budget", source = "budget", qualifiedByName = "toArs")
    OrderToSubmit toOrderSubmit(SubmitOrderRequest request);

    OrderResponse toResponse(Order order);

    @Mapping(target = "size", source = "size", qualifiedByName = "toDomainSize")
    Order toOrderDomain(OrderEntity order);

    @Mapping(target = "size", source = "size", qualifiedByName = "toEntitySize")
    OrderEntity toOrderEntity(Order order);

    @Named("toEntitySize")
    default Integer toEntitySize(Long size) {
        return size == null ? null : Math.toIntExact(size);
    }

    @Named("toDomainSize")
    default Long toDomainSize(Integer size) {
        return size == null ? null : size.longValue();
    }

    @Named("toArs")
    default Money.ARS toArs(BigDecimal value) {
        return value == null ? new Money.ARS(BigDecimal.ZERO) : new Money.ARS(value);
    }
}
