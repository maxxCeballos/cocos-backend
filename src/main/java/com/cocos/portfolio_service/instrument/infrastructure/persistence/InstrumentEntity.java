package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class InstrumentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(length = 10)
    private String ticker;
    @Column(length = 255)
    private String name;
    @Column(length = 10)
    private String type;

    protected InstrumentEntity() {}

    public Integer getId() { return id; }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public String getType() { return type; }
}
