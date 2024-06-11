package org.citrus.learn.positionsservice.client.impl;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.citrus.learn.positionsservice.client.PricesProvider;
import org.springframework.stereotype.Component;

@Component
// TODO: Remove
public class ExternalApiPricesProvider implements PricesProvider {
	private final SecureRandom secureRandom = new SecureRandom();

	@Override
	public Map<String, BigDecimal> fetchPricesForSymbols(Set<String> symbols) {
		return symbols.stream().collect(Collectors.toMap(v -> v, v -> BigDecimal.valueOf(secureRandom.nextDouble(500))));
	}
}
