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
									.symbol("XBT/USD")
									.purchasePrice(BigDecimal.valueOf(123D))
									.quantity(BigDecimal.valueOf(1))
									.build(),
							Position.newBuilder()
									.symbol("RPL/USD")
									.purchasePrice(BigDecimal.valueOf(99D))
									.quantity(BigDecimal.valueOf(5))
									.build()
					)));
		}).thenApply(positionsByClientId -> positionsByClientId.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> {
					try {
						return positionsEnrichers.stream()
								.filter(i -> i.shouldBeApplied(getContext(environment, v.getKey())))
								.reduce(CompletableFuture.completedFuture(v.getValue()),
										(future, enricher) -> enricher.enrich(future, getContext(environment, v.getKey())), (a, b) -> b)
								.get();
					}
					catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				})));
	}

	private PositionDetailsLoadContext getContext(BatchLoaderEnvironment environment, String key) {
		return (PositionDetailsLoadContext) environment.getKeyContexts().get(key);
	}

}
