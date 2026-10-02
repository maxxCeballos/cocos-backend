package com.cocos.portfolio_service.instrument.infrastructure.persistence;

import com.cocos.portfolio_service.marketdata.infrastructure.persistence.MarketDataJpaRepository;
import com.cocos.portfolio_service.order.domain.enums.OrderStatus;
import com.cocos.portfolio_service.order.infrastructure.persistence.OrderJpaRepository;
import com.cocos.portfolio_service.user.infrastructure.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class DomainJpaRepositoryTest {
    @Autowired private EntityManager entityManager;
    @Autowired private UserJpaRepository users;
    @Autowired private InstrumentJpaRepository instruments;
    @Autowired private MarketDataJpaRepository marketData;
    @Autowired private OrderJpaRepository orders;

    @BeforeEach
    void insertDatabaseRows() {
        entityManager.createNativeQuery("insert into users (email, accountnumber) values ('user@test.com', '10001')")
                .executeUpdate();
        entityManager.createNativeQuery("insert into instruments (ticker, name, type) values ('GGAL', 'Grupo Galicia', 'ACCIONES')")
                .executeUpdate();
        entityManager.createNativeQuery("insert into marketdata (instrumentid, close, previousclose, date) values (1, 95.00, 90.00, :date)")
                .setParameter("date", LocalDate.of(2026, 9, 28)).executeUpdate();
        entityManager.createNativeQuery("insert into marketdata (instrumentid, close, previousclose, date) values (1, 100.00, 95.00, :date)")
                .setParameter("date", LocalDate.of(2026, 9, 29)).executeUpdate();
        entityManager.createNativeQuery("insert into orders (instrumentid, userid, size, price, type, side, status, datetime) " +
                        "values (1, 1, 2, 100.00, 'MARKET', 'BUY', 'FILLED', current_timestamp)")
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void whenRowsExist_thenJpaRepositories_mapActualDatabaseColumns() {
        // ARRANGE

        // ACT
        var userExists = users.existsById(1L);
        var instrumentPage = instruments.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(
                "gal", "gal", org.springframework.data.domain.PageRequest.of(0, 10));
        var latestPrice = marketData.findFirstByInstrumentIdOrderByDateDescIdDesc(1L).orElseThrow();
        var filledOrders = orders.findByUserId(1L, OrderStatus.FILLED);

        // ASSERT
        assertTrue(userExists);
        assertEquals("GGAL", instrumentPage.getContent().getFirst().getTicker());
        assertEquals("100.00", latestPrice.getClose().toPlainString());
        assertEquals(1L, filledOrders.getFirst().toDomain().id());
    }
}
