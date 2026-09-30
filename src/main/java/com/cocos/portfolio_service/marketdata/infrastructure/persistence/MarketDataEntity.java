package com.cocos.portfolio_service.marketdata.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "marketdata")
public class MarketDataEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "instrumentid")
    private Integer instrumentId;
    @Column(precision = 10, scale = 2)
    private BigDecimal close;
    @Column(name = "previousclose", precision = 10, scale = 2)
    private BigDecimal previousClose;
    private LocalDate date;

    protected MarketDataEntity() {}

    public Integer getId() { return id; }
    public Integer getInstrumentId() { return instrumentId; }
    public BigDecimal getClose() { return close; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public LocalDate getDate() { return date; }
}
