package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "instruments")
@Getter
public class InstrumentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10)
    private String ticker;

    @Column(length = 255)
    private String name;

    @Column(length = 10)
    private String type;
}
