package org.citrus.learn.accountbalanceservice.rest.handler;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.citrus.learn.accountbalanceservice.service.AccountBalanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.server.ServerRequest;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TestUserBalanceRequestHandler {
	private static final String VALID_ACCOUNT_ID = "123";
	private static final String VALID_CURRENCY = "EUR";

	@Mock
	AccountBalanceService accountBalanceService;

	@InjectMocks
	UserBalanceRequestHandler userBalanceRequestHandler;

	@Mock
	ServerRequest request;

	@Test
	void shouldReturnBadRequestIfAccountIdIsNotInteger() {
		when(request.pathVariable("accountId")).thenReturn("hey");
		StepVerifier.create(userBalanceRequestHandler.fetchBalanceInCurrency(request))
				.expectNextMatches(response -> response.statusCode() == HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()))
				.expectComplete()
				.log()
				.verify();
	}

	@ParameterizedTest
	@ValueSource(strings = {"uSD", "xxxx", "12", "USd", "IIII"})
	void shouldReturnBadRequestIfCurrencyCodeIsNotInIsoFormat(String invalidCurrency) {
		when(request.pathVariable("accountId")).thenReturn(VALID_ACCOUNT_ID);
		when(request.pathVariable("currency")).thenReturn(invalidCurrency);
		StepVerifier.create(userBalanceRequestHandler.fetchBalanceInCurrency(request))
				.expectNextMatches(response -> response.statusCode() == HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()))
				.expectComplete()
				.log()
				.verify();
	}

	@Test
	void shouldReturnNotFoundIfAccountCouldNotBeFound() {
		when(request.pathVariable("accountId")).thenReturn(VALID_ACCOUNT_ID);
		when(request.pathVariable("currency")).thenReturn(VALID_CURRENCY);
		when(accountBalanceService.findBalanceInSpecificCurrency(Long.parseLong(VALID_ACCOUNT_ID), VALID_CURRENCY))
				.thenReturn(Mono.empty());
		StepVerifier.create(userBalanceRequestHandler.fetchBalanceInCurrency(request))
				.expectNextMatches(response -> response.statusCode() == HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()))
				.expectComplete()
				.log()
				.verify();
	}

	@Test
	void shouldReturnOkResponseIfCalculationWasSuccessful() {
		when(request.pathVariable("accountId")).thenReturn(VALID_ACCOUNT_ID);
		when(request.pathVariable("currency")).thenReturn(VALID_CURRENCY);
		when(accountBalanceService.findBalanceInSpecificCurrency(Long.parseLong(VALID_ACCOUNT_ID), VALID_CURRENCY))
				.thenReturn(Mono.just(BigDecimal.TEN));
		StepVerifier.create(userBalanceRequestHandler.fetchBalanceInCurrency(request))
				.expectNextMatches(response -> response.statusCode() == HttpStatusCode.valueOf(HttpStatus.OK.value()))
				.expectComplete()
				.log()
				.verify();
	}

}
