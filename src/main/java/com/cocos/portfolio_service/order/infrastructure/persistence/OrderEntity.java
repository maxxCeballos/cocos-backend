package com.cocos.portfolio_service.order.infrastructure.persistence;

import com.cocos.portfolio_service.order.domain.enums.OrderSide;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.domain.enums.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "instrumentid")
    private Integer instrumentId;
    @Column(name = "userid")
    private Integer userId;
    private Integer size;
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OrderType type;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OrderSide side;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderStatus status;
    @Column(name = "datetime")
    private LocalDateTime datetime;

    protected OrderEntity() {}

    public static OrderEntity fromDomain(com.cocos.portfolio_service.order.domain.Order order) {
        var entity = new OrderEntity();
        entity.id = order.id() == null ? null : Math.toIntExact(order.id());
        entity.instrumentId = Math.toIntExact(order.instrumentId());
        entity.userId = Math.toIntExact(order.userId());
        entity.size = Math.toIntExact(order.quantity());
        entity.price = order.price();
        entity.type = order.type();
        entity.side = order.side();
        entity.status = order.status();
        entity.datetime = order.datetime() == null ? null : LocalDateTime.ofInstant(order.datetime(), ZoneOffset.UTC);
        return entity;
    }

    public com.cocos.portfolio_service.order.domain.Order toDomain() {
        return new com.cocos.portfolio_service.order.domain.Order(id.longValue(), userId.longValue(),
                instrumentId.longValue(), side, size.longValue(), price, type, status,
                datetime == null ? null : datetime.toInstant(ZoneOffset.UTC));
    }
}
