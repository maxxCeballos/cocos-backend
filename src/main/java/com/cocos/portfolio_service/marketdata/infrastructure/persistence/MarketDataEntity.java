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
    private Long id;
    @Column(name = "instrumentid")
    private Long instrumentId;
    private BigDecimal close;
    @Column(name = "previousclose")
    private BigDecimal previousClose;
    private LocalDate date;

    protected MarketDataEntity() {}

    public Long getId() { return id; }
    public Long getInstrumentId() { return instrumentId; }
    public BigDecimal getClose() { return close; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public LocalDate getDate() { return date; }
}
