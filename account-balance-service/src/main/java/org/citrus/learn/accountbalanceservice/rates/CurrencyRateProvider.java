package org.citrus.learn.accountbalanceservice.rates;

import java.math.BigDecimal;

import reactor.core.publisher.Mono;

public interface CurrencyRateProvider {
	Mono<BigDecimal> getRate(String fromCurrency, String toCurrency);
}
