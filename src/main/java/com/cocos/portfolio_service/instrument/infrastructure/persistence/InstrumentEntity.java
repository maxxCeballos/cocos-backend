package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import com.cocos.portfolio_service.instrument.domain.enums.InstrumentType;
import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private InstrumentType type;
}
