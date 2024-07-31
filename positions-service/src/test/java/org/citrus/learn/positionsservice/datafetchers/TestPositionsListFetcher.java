package org.citrus.learn.positionsservice.datafetchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.graphql.datafetchers.PositionsListFetcher;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import graphql.schema.DataFetchingEnvironment;

@ExtendWith(MockitoExtension.class)
class TestPositionsListFetcher {
	private static final ClientId CLIENT_ID = ClientId.newBuilder().id("123").build();
	private static final List<Position> POSITIONS = List.of(Position.newBuilder().build());
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	@Mock
	DataFetchingEnvironment environment;

	@Mock
	DataLoader<String, List<Position>> positionsDataLoader;

	PositionsListFetcher positionsListFetcher = new PositionsListFetcher();

	@Test
	void shouldDelegateFetchOfPositionsToDataLoader() {
		var positionDetailsLoadContext = PositionDetailsLoadContext.builder()
				.clientId(CLIENT_ID)
				.build();
		when(environment.getLocalContext()).thenReturn(positionDetailsLoadContext);
		when(environment.<String, List<Position>> getDataLoader("positions")).thenReturn(positionsDataLoader);
		when(positionsDataLoader.load(CLIENT_ID.getId(), positionDetailsLoadContext)).thenReturn(CompletableFuture.completedFuture(POSITIONS));
		assertThat(positionsListFetcher.fetchPositions(environment)).succeedsWithin(TIMEOUT).isEqualTo(POSITIONS);
	}
}
