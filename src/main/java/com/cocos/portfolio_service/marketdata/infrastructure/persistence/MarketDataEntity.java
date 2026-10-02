package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "marketdata")
@Getter
public class MarketDataEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumentid")
    private Long instrumentId;

    @Column(precision = 10, scale = 2)
    private BigDecimal close;

    @Column(name = "previousclose", precision = 10, scale = 2)
    private BigDecimal previousClose;

    private LocalDate date;
}
