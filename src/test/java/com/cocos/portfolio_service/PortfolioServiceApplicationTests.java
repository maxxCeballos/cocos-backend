package com.cocos.portfolio_service;

import com.cocos.portfolio_service.instrument.application.InstrumentSearch;
import com.cocos.portfolio_service.order.application.OrderCommands;
import com.cocos.portfolio_service.portfolio.application.PortfolioQuery;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PortfolioServiceApplicationTests {

	@MockitoBean
	private PortfolioQuery portfolioQuery;

	@MockitoBean
	private InstrumentSearch instrumentSearch;

	@MockitoBean
	private OrderCommands orderCommands;

	@Test
	void contextLoads() {
	}

}
