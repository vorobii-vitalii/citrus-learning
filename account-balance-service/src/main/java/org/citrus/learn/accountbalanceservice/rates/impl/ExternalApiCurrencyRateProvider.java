package org.citrus.learn.accountbalanceservice.rates.impl;

import java.math.BigDecimal;

import org.citrus.learn.accountbalanceservice.rates.CurrencyRateProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class ExternalApiCurrencyRateProvider implements CurrencyRateProvider {
	private final WebClient webClient;

	public ExternalApiCurrencyRateProvider(@Qualifier("externalRateClient") WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public Mono<BigDecimal> getRate(String fromCurrency, String toCurrency) {
		return webClient.get()
				.uri("/rates/{from}/{to}", fromCurrency, toCurrency)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(RatesResponse.class)
				.map(RatesResponse::rate);
	}

	private record RatesResponse(BigDecimal rate) {
	}

}
