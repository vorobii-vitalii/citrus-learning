package org.citrus.learn.positionsservice.enricher.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.prices.PricesProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestPositionsCurrentPricesAsyncEnricher {
	private static final Duration TIMEOUT = Duration.ofSeconds(2L);

	@Mock
	PricesProvider pricesProvider;

	@InjectMocks
	PositionsCurrentPricesAsyncEnricher enricher;

	@Test
	void shouldEnrichPositionsWithPrices() {
		List<Position> positions = List.of(
				Position.newBuilder()
						.symbol("XBT/USD")
						.quantity(BigDecimal.ONE)
						.build(),
				Position.newBuilder()
						.symbol("SOL/EUR")
						.quantity(BigDecimal.ONE)
						.build(),
				Position.newBuilder()
						.symbol("XBT/USD")
						.quantity(BigDecimal.valueOf(2L))
						.build());
		when(pricesProvider.fetchPricesForSymbols(Set.of("XBT/USD", "SOL/EUR")))
				.thenReturn(Map.of(
						"XBT/USD", BigDecimal.valueOf(44),
						"SOL/EUR", BigDecimal.valueOf(120)
				));
		assertThat(enricher.enrich(CompletableFuture.completedFuture(positions), createContext(true)))
				.succeedsWithin(TIMEOUT)
				.isEqualTo(List.of(
						Position.newBuilder()
								.symbol("XBT/USD")
								.quantity(BigDecimal.ONE)
								.currentPrice(BigDecimal.valueOf(44))
								.build(),
						Position.newBuilder()
								.symbol("SOL/EUR")
								.quantity(BigDecimal.ONE)
								.currentPrice(BigDecimal.valueOf(120))
								.build(),
						Position.newBuilder()
								.symbol("XBT/USD")
								.quantity(BigDecimal.valueOf(2L))
								.currentPrice(BigDecimal.valueOf(44))
								.build()
				));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void shouldBeApplied(boolean shouldFetchCurrentPrices) {
		var context = createContext(shouldFetchCurrentPrices);
		assertThat(enricher.shouldBeApplied(context)).isEqualTo(shouldFetchCurrentPrices);
	}

	private PositionDetailsLoadContext createContext(boolean shouldFetchCurrentPrices) {
		return PositionDetailsLoadContext.builder().fetchPositionsCurrentPrices(shouldFetchCurrentPrices).build();
	}
}
