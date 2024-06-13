package org.citrus.learn.positionsservice.datafetchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.graphql.datafetchers.PositionsPerformanceFetcher;
import org.citrus.learn.positionsservice.service.PositionsPerformanceEvaluator;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import graphql.schema.DataFetchingEnvironment;

@ExtendWith(MockitoExtension.class)
class TestPositionsPerformanceFetcher {
	private static final BigDecimal PERFORMANCE = BigDecimal.TEN;
	private static final ClientId CLIENT_ID = ClientId.newBuilder().id("123").build();
	private static final List<Position> POSITIONS = List.of(Position.newBuilder().build());
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	@Mock
	PositionsPerformanceEvaluator positionsPerformanceEvaluator;

	@InjectMocks
	PositionsPerformanceFetcher positionsPerformanceFetcher;

	@Mock
	DataFetchingEnvironment environment;

	@Mock
	DataLoader<String, List<Position>> positionsDataLoader;

	@Test
	void shouldDelegatePerformanceCalculationToPerformanceEvaluatorAfterFetchOfPositions() {
		var positionDetailsLoadContext = PositionDetailsLoadContext.builder()
				.clientId(CLIENT_ID)
				.build();
		when(environment.getLocalContext()).thenReturn(positionDetailsLoadContext);
		when(environment.<String, List<Position>> getDataLoader("positions")).thenReturn(positionsDataLoader);
		when(positionsDataLoader.load(CLIENT_ID.getId(), positionDetailsLoadContext)).thenReturn(CompletableFuture.completedFuture(POSITIONS));
		when(positionsPerformanceEvaluator.evaluatePositionsPerformance(POSITIONS))
				.thenReturn(PERFORMANCE);

		assertThat(positionsPerformanceFetcher.fetchPerformance(environment)).succeedsWithin(TIMEOUT).isEqualTo(PERFORMANCE);
	}
}
