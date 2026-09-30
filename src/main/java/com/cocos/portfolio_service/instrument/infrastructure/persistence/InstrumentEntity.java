package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class InstrumentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticker;
    private String name;
    private String type;

    protected InstrumentEntity() {}

    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public String getType() { return type; }
}
