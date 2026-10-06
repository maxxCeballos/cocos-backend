package com.cocos.portfolio_service.shared.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public sealed interface Money permits Money.ARS, Money.USD {

    BigDecimal value();

    record ARS(BigDecimal value) implements Money {
        public static final String CURRENCY_LABEL = "AR$";

        public ARS {
            if (value == null) throw new IllegalArgumentException("El valor no puede ser nulo");
        }

        public ARS add(ARS other) {
            return new ARS(this.value.add(other.value));
        }

        public ARS subtract(ARS other) {
            BigDecimal result = this.value.subtract(other.value);
            return new ARS(result);
        }

        public ARS multiply(BigDecimal factor) {
            return new ARS(this.value.multiply(factor));
        }

        public USD toUsd(BigDecimal arsToUsdRate) {
            BigDecimal converted = this.value.multiply(arsToUsdRate);
            return new USD(converted);
        }

        @Override
        public String toString() {
            BigDecimal displayValue = this.value.setScale(2, RoundingMode.HALF_EVEN);
            return "$" + displayValue;
        }
    }

    record USD(BigDecimal value) implements Money {
        public USD {
            if (value == null) throw new IllegalArgumentException("El valor no puede ser nulo");
        }

        public ARS toArs(BigDecimal usdToArsRate) {
            BigDecimal converted = this.value.multiply(usdToArsRate);
            return new ARS(converted);
        }

        @Override
        public String toString() {
            BigDecimal displayValue = this.value.setScale(2, RoundingMode.HALF_EVEN);
            return "US$" + displayValue;
        }
    }
}
