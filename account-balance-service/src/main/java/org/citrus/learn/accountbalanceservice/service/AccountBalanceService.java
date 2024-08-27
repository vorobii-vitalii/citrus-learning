package org.citrus.learn.accountbalanceservice.service;

import java.math.BigDecimal;

import reactor.core.publisher.Mono;

public interface AccountBalanceService {
	Mono<BigDecimal> findBalanceInSpecificCurrency(long accountId, String currency);
}
