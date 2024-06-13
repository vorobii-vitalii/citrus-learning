package org.citrus.learn.positionsservice.datafetchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.service.PositionsPerformanceEvaluator;
import org.dataloader.DataLoader;
import org.jetbrains.annotations.NotNull;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DgsComponent
@RequiredArgsConstructor
public class PositionsPerformanceFetcher {
	private final PositionsPerformanceEvaluator positionsPerformanceEvaluator;

	@DgsData(field = DgsConstants.POSITIONDETAILS.Performance, parentType = DgsConstants.POSITIONDETAILS.TYPE_NAME)
	public CompletableFuture<BigDecimal> fetchPerformance(DataFetchingEnvironment environment) {
		PositionDetailsLoadContext positionDetailsLoadContext = Objects.requireNonNull(environment.getLocalContext());
		DataLoader<String, List<Position>> positionLoader = getPositionsDataLoader(environment);
		return positionLoader.load(positionDetailsLoadContext.clientId().getId(), positionDetailsLoadContext)
				.thenApply(positionsPerformanceEvaluator::evaluatePositionsPerformance);
	}

	private @NotNull DataLoader<String, List<Position>> getPositionsDataLoader(DataFetchingEnvironment environment) {
		return Objects.requireNonNull(environment.getDataLoader("positions"));
	}

}
