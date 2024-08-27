package org.citrus.learn.accountbalanceservice.service.impl;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.citrus.learn.accountbalanceservice.dao.ReactiveUserBalanceRepository;
import org.citrus.learn.accountbalanceservice.entity.UserBalance;
import org.citrus.learn.accountbalanceservice.rates.CurrencyRateProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TestAccountBalanceServiceImpl {
	private static final long USER_ID = 123L;
	private static final String EUR = "EUR";
	private static final String USD = "USD";

	@Mock
	ReactiveUserBalanceRepository userBalanceRepository;

	@Mock
	CurrencyRateProvider currencyRateProvider;

	@InjectMocks
	AccountBalanceServiceImpl accountBalanceService;

	@Test
	void shouldTakeIntoAccountRateWhenComputingBalance() {
		when(userBalanceRepository.findByUserId(USER_ID))
				.thenReturn(Mono.just(UserBalance.builder()
						.userId(USER_ID)
						.balance(new BigDecimal("900.5"))
						.build()));
		when(currencyRateProvider.getRate(USD, EUR)).thenReturn(Mono.just(new BigDecimal("1.2")));
		StepVerifier.create(accountBalanceService.findBalanceInSpecificCurrency(USER_ID, EUR))
				.expectNext(new BigDecimal("1080.60"))
				.expectComplete()
				.log()
				.verify();
	}

	@Test
	void shouldReturnEmptyResultIfAccountNotFound() {
		when(userBalanceRepository.findByUserId(USER_ID)).thenReturn(Mono.empty());
		StepVerifier.create(accountBalanceService.findBalanceInSpecificCurrency(USER_ID, EUR))
				.expectComplete()
				.log()
				.verify();
		verifyNoInteractions(currencyRateProvider);
	}

	@Test
	void shouldReturnErrorIfRateWasNotFound() {
		when(userBalanceRepository.findByUserId(USER_ID)).thenReturn(Mono.just(UserBalance.builder()
				.userId(USER_ID)
				.balance(BigDecimal.TEN)
				.build()));
		when(currencyRateProvider.getRate(USD, EUR)).thenReturn(Mono.error(new RuntimeException()));
		StepVerifier.create(accountBalanceService.findBalanceInSpecificCurrency(USER_ID, EUR))
				.expectError()
				.log()
				.verify();
	}

}
