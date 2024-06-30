package org.citrus.learn.positionsservice.graphql.dataloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.dao.ClientPositionsRepository;
import org.citrus.learn.positionsservice.enricher.AsyncEnricher;
import org.dataloader.BatchLoaderEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TestPositionsDataLoader {
	private static final Duration TIMEOUT = Duration.ofSeconds(2);
	private static final String CLIENT_ID = "123";

	@Mock
	AsyncEnricher<List<Position>, PositionDetailsLoadContext> enricher;

	@Mock
	ClientPositionsRepository clientPositionsRepository;

	PositionsDataLoader positionsDataLoader;

	@Mock
	BatchLoaderEnvironment environment;

	@BeforeEach
	void init() {
		positionsDataLoader = new PositionsDataLoader(List.of(enricher), clientPositionsRepository);
	}

	@Test
	void shouldApplyEnrichersIfNeeded() {
		var positions = List.of(Position.newBuilder()
				.symbol("ABBN")
				.purchasePrice(BigDecimal.ONE)
				.quantity(BigDecimal.ONE)
				.build());
		var enrichedPositions = List.of(Position.newBuilder()
				.symbol("ABBN")
				.purchasePrice(BigDecimal.ONE)
				.quantity(BigDecimal.ONE)
				.currentPrice(BigDecimal.valueOf(222))
				.build());
		var context = createContext(true);
		when(environment.getKeyContexts()).thenReturn(Map.of(CLIENT_ID, context));
		when(clientPositionsRepository.findPositionsForClients(Set.of(CLIENT_ID)))
				.thenReturn(Mono.just(Map.of(CLIENT_ID, positions)));
		when(enricher.shouldBeApplied(context)).thenReturn(true);
		when(enricher.enrich(any(), eq(context))).thenReturn(CompletableFuture.completedFuture(enrichedPositions));
		assertThat(positionsDataLoader.load(Set.of(CLIENT_ID), environment))
				.succeedsWithin(TIMEOUT)
				.isEqualTo(Map.of(CLIENT_ID, enrichedPositions));
	}

	@Test
	void shouldNotApplyEnrichersIfNotNeeded() {
		var positions = List.of(Position.newBuilder()
				.symbol("ABBN")
				.purchasePrice(BigDecimal.ONE)
				.quantity(BigDecimal.ONE)
				.build());
		var context = createContext(false);
		when(environment.getKeyContexts()).thenReturn(Map.of(CLIENT_ID, context));
		when(clientPositionsRepository.findPositionsForClients(Set.of(CLIENT_ID)))
				.thenReturn(Mono.just(Map.of(CLIENT_ID, positions)));
		when(enricher.shouldBeApplied(context)).thenReturn(false);
		assertThat(positionsDataLoader.load(Set.of(CLIENT_ID), environment))
				.succeedsWithin(TIMEOUT)
				.isEqualTo(Map.of(CLIENT_ID, positions));
	}

	private PositionDetailsLoadContext createContext(boolean fetchPrices) {
		return PositionDetailsLoadContext.builder().fetchPositionsCurrentPrices(fetchPrices).build();
	}

}
