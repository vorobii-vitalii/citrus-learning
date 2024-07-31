package org.citrus.learn.positionsservice.graphql.dataloader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.dao.ClientPositionsRepository;
import org.citrus.learn.positionsservice.enricher.AsyncEnricher;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.MappedBatchLoaderWithContext;
import org.springframework.data.util.Pair;

import com.netflix.graphql.dgs.DgsDataLoader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@DgsDataLoader(name = "positions")
@RequiredArgsConstructor
public class PositionsDataLoader implements MappedBatchLoaderWithContext<String, List<Position>> {
	private final List<AsyncEnricher<List<Position>, PositionDetailsLoadContext>> positionsEnrichers;
	private final ClientPositionsRepository clientPositionsRepository;

	@Override
	public CompletionStage<Map<String, List<Position>>> load(Set<String> clientIds, BatchLoaderEnvironment environment) {
		log.info("Fetching positions for {}", clientIds);
		return clientPositionsRepository.findPositionsForClients(clientIds).toFuture()
				.thenCompose(positionsByClientId -> Flux.fromIterable(positionsByClientId.entrySet())
						.subscribeOn(Schedulers.boundedElastic())
						.flatMap(entry -> {
							var clientId = entry.getKey();
							var positions = entry.getValue();
							var context = getContext(environment, clientId);
							return Mono.fromCompletionStage(() -> {
								log.info("Positions before applying enrichers = {}", positions);
								return applyEnrichers(context, positions);
							}).map(p -> Pair.of(clientId, p));
						})
						.collectMap(Pair::getFirst, Pair::getSecond)
						.toFuture());
	}

	private CompletableFuture<List<Position>> applyEnrichers(PositionDetailsLoadContext context, List<Position> positions) {
		return positionsEnrichers.stream()
				.filter(i -> i.shouldBeApplied(context))
				.reduce(CompletableFuture.completedFuture(positions), (future, enricher) -> enricher.enrich(future, context), (a, b) -> b);
	}

	private PositionDetailsLoadContext getContext(BatchLoaderEnvironment environment, String key) {
		return (PositionDetailsLoadContext) environment.getKeyContexts().get(key);
	}

}
