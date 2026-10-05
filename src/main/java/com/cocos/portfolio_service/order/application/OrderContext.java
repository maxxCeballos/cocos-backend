package com.cocos.portfolio_service.order.application;

import com.cocos.portfolio_service.instrument.domain.Instrument;
import com.cocos.portfolio_service.user.domain.User;

public record OrderContext(
        User user,
        Instrument instrument
) { }
