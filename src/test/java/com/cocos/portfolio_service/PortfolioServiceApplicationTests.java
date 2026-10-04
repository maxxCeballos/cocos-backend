package com.cocos.portfolio_service;

import com.cocos.portfolio_service.instrument.application.IInstrument;
import com.cocos.portfolio_service.order.application.ports.IOrderService;
import com.cocos.portfolio_service.portfolio.application.IPortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PortfolioServiceApplicationTests {

	@MockitoBean
	private IPortfolioService portfolioQuery;

	@MockitoBean
	private IInstrument instrumentSearch;

	@MockitoBean
	private IOrderService orderCommands;

	@Test
	void contextLoads() {
	}

}
