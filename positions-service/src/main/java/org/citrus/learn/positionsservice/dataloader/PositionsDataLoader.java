package org.citrus.learn.positionsservice.dataloader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.enricher.AsyncEnricher;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.MappedBatchLoaderWithContext;


import com.netflix.graphql.dgs.DgsDataLoader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DgsDataLoader(name = "positions")
@RequiredArgsConstructor
public class PositionsDataLoader implements MappedBatchLoaderWithContext<String, List<Position>> {
	private final List<AsyncEnricher<List<Position>, PositionDetailsLoadContext>> positionsEnrichers;

	@Override
	public CompletionStage<Map<String, List<Position>>> load(Set<String> clientIds, BatchLoaderEnvironment environment) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Fetching positions for {}", clientIds);
			// TODO: Integrate Mongo
			return clientIds.stream()
					.collect(Collectors.toMap(v -> v, v -> List.of(
							Position.newBuilder()
									.id("1")
									.symbol("XBT/USD")
									.purchaseDate("2024/06/01")
									.purchasePrice(BigDecimal.valueOf(123D))
									.quantity(BigDecimal.valueOf(1))
									.build(),
							Position.newBuilder()
									.id("2")
									.symbol("RPL/USD")
									.purchaseDate("2024/06/03")
									.purchasePrice(BigDecimal.valueOf(99D))
									.quantity(BigDecimal.valueOf(5))
									.build()
					)));
		}).thenApply(positionsByClientId -> positionsByClientId.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> {
					try {
						return positionsEnrichers.stream()
								.filter(i -> i.shouldBeApplied((PositionDetailsLoadContext) environment.getKeyContexts().get(v.getKey())))
								.reduce(CompletableFuture.completedFuture(v.getValue()),
										(future, enricher) -> enricher.enrich(future,
												(PositionDetailsLoadContext) environment.getKeyContexts().get(v.getKey()))
										, (a, b) -> b)
								.get();
					}
					catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				})));
	}

}
