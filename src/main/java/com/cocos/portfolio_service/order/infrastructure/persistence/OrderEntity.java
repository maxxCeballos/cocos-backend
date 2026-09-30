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
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "instrumentid")
    private Long instrumentId;
    @Column(name = "userid")
    private Long userId;
    private Long size;
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private OrderType type;
    @Enumerated(EnumType.STRING)
    private OrderSide side;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Column(name = "datetime")
    private Instant datetime;

    protected OrderEntity() {}

    public static OrderEntity fromDomain(com.cocos.portfolio_service.order.domain.Order order) {
        var entity = new OrderEntity();
        entity.id = order.id();
        entity.instrumentId = order.instrumentId();
        entity.userId = order.userId();
        entity.size = order.quantity();
        entity.price = order.price();
        entity.type = order.type();
        entity.side = order.side();
        entity.status = order.status();
        entity.datetime = order.datetime();
        return entity;
    }

    public com.cocos.portfolio_service.order.domain.Order toDomain() {
        return new com.cocos.portfolio_service.order.domain.Order(id, userId, instrumentId, side, size,
                price, type, status, datetime);
    }
}
