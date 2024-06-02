package org.citrus.learn.accountbalanceservice.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.citrus.learn.accountbalanceservice.dao.ReactiveUserBalanceRepository;
import org.citrus.learn.accountbalanceservice.entity.UserBalance;
import org.citrus.learn.accountbalanceservice.rates.CurrencyRateProvider;
import org.citrus.learn.accountbalanceservice.service.AccountBalanceService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceServiceImpl implements AccountBalanceService {
	protected static final String USD = "USD";
	public static final int RES_PRECISION = 2;

	private final ReactiveUserBalanceRepository userBalanceRepository;
	private final CurrencyRateProvider currencyRateProvider;

	@Override
	public Mono<BigDecimal> findBalanceInSpecificCurrency(long accountId, String currency) {
		log.info("Fetching balance of account {} in {}", accountId, currency);
		return userBalanceRepository.findByUserId(accountId)
				.map(UserBalance::getBalance)
				.flatMap(balanceInUSD -> {
					log.info("Balance of {} in USD = {}", accountId, balanceInUSD);
					return currencyRateProvider.getRate(USD, currency)
							.map(rate -> {
								log.info("Rate USD/{} = {}", currency, rate);
								return balanceInUSD.multiply(rate).setScale(RES_PRECISION, RoundingMode.FLOOR);
							});
				});
	}
}
