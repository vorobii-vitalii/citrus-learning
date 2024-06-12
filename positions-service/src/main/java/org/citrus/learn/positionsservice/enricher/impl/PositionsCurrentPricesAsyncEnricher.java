package org.citrus.learn.positionsservice.enricher.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.citrus.learn.positionsservice.client.PricesProvider;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.enricher.AsyncEnricher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class PositionsCurrentPricesAsyncEnricher implements AsyncEnricher<List<Position>, PositionDetailsLoadContext> {
	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final PricesProvider pricesProvider;

	@Override
	public CompletableFuture<List<Position>> enrich(CompletableFuture<List<Position>> existingFuture, PositionDetailsLoadContext context) {
		return existingFuture.thenCompose(existing -> {
			Set<String> distinctSymbols = existing.stream().map(Position::getSymbol).collect(Collectors.toSet());
			log.info("Going to enrich positions with current prices...");
			return CompletableFuture.supplyAsync(() -> pricesProvider.fetchPricesForSymbols(distinctSymbols), executorService)
					.thenApply(priceBySymbol -> {
						log.info("Fetched prices = {}", priceBySymbol);
						return existing.stream()
								.peek(position -> {
									var symbol = position.getSymbol();
									var currentPrice = priceBySymbol.get(symbol);
									if (currentPrice == null) {
										log.warn("No price for {}", symbol);
									} else {
										// TODO: Change!
										position.setCurrentPrice(currentPrice.doubleValue());
									}
								})
								.toList();
					});
		});
	}

	@Override
	public boolean shouldBeApplied(PositionDetailsLoadContext context) {
		log.info("Going to add position prices = {}", context.fetchPositionsCurrentPrices());
		return context.fetchPositionsCurrentPrices();
	}
}
