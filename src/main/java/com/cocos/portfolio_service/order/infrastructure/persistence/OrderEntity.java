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
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "orders")
@Getter
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumentid")
    private Long instrumentId;

    @Column(name = "userid")
    private Long userId;

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

    public static OrderEntity fromDomain(com.cocos.portfolio_service.order.domain.Order order) {
        var entity = new OrderEntity();
        entity.id = order.id() == null ? null : order.id();
        entity.instrumentId = order.instrumentId();
        entity.userId = order.userId();
        entity.size = Math.toIntExact(order.size());
        entity.price = order.price();
        entity.type = order.type();
        entity.side = order.side();
        entity.status = order.status();
        entity.datetime = order.datetime() == null ? null : order.datetime();
        return entity;
    }

    public com.cocos.portfolio_service.order.domain.Order toDomain() {
        return new com.cocos.portfolio_service.order.domain.Order(id, userId,
                instrumentId, side, size.longValue(), price, type, status,
                datetime == null ? null : LocalDateTime.from(datetime.toInstant(ZoneOffset.UTC)));
    }
}
