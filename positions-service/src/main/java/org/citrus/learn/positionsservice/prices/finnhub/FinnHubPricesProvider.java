package org.citrus.learn.positionsservice.prices.finnhub;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.citrus.learn.positionsservice.prices.PricesProvider;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class FinnHubPricesProvider implements PricesProvider {
	private final QuotesService quotesService;

	@SneakyThrows
	@Override
	public Map<String, BigDecimal> fetchPricesForSymbols(Set<String> symbols) {
		var response = quotesService.fetchQuotes(new ArrayList<>(symbols)).execute();
		log.info("Response from FinnHub API = {}", response);
		List<Quote> fetchedQuotes = Objects.requireNonNull(response.body())
				.stream()
				.filter(Objects::nonNull)
				.toList();
		log.info("Quotes = {}", fetchedQuotes);
		return fetchedQuotes.stream().collect(Collectors.toMap(Quote::symbol, Quote::unitPrice));
	}
}
