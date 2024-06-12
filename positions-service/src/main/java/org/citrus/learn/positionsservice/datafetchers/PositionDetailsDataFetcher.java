package org.citrus.learn.positionsservice.datafetchers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.codegen.types.PositionDetails;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.dataloader.DataLoader;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

@DgsComponent
@Slf4j
public class PositionDetailsDataFetcher {

	public static final MathContext MATH_CONTEXT = new MathContext(8);

	@DgsQuery(field = DgsConstants.QUERY.PositionDetails)
	public DataFetcherResult<PositionDetails> getPositionDetails(
			DataFetchingEnvironment dataFetchingEnvironment,
			@InputArgument(name = DgsConstants.QUERY.POSITIONDETAILS_INPUT_ARGUMENT.ClientId) ClientId clientId
	) {
		log.info("Fetching positions of client = {} position details", clientId);
		boolean shouldFetchPrices = dataFetchingEnvironment.getSelectionSet().contains(DgsConstants.POSITIONDETAILS.Performance);
		var positionDetailsLoadContext = new PositionDetailsLoadContext(clientId, shouldFetchPrices);
		log.info("Calculated position details load context = {}", positionDetailsLoadContext);
		return DataFetcherResult.<PositionDetails> newResult()
				.data(new PositionDetails())
				.localContext(positionDetailsLoadContext)
				.build();
	}

	@DgsData(field = DgsConstants.POSITIONDETAILS.Positions, parentType = DgsConstants.POSITIONDETAILS.TYPE_NAME)
	public CompletableFuture<List<Position>> fetchPositions(DataFetchingEnvironment environment) {
		PositionDetailsLoadContext positionDetailsLoadContext = Objects.requireNonNull(environment.getLocalContext());
		DataLoader<String, List<Position>> positionLoader = Objects.requireNonNull(environment.getDataLoader("positions"));
		return positionLoader.load(positionDetailsLoadContext.clientId().getId(), positionDetailsLoadContext);
	}

	@DgsData(field = DgsConstants.POSITIONDETAILS.Performance, parentType = DgsConstants.POSITIONDETAILS.TYPE_NAME)
	public CompletableFuture<BigDecimal> fetchPerformance(DataFetchingEnvironment environment) {
		PositionDetailsLoadContext positionDetailsLoadContext = Objects.requireNonNull(environment.getLocalContext());
		DataLoader<String, List<Position>> positionLoader = Objects.requireNonNull(environment.getDataLoader("positions"));
		return positionLoader.load(positionDetailsLoadContext.clientId().getId(), positionDetailsLoadContext)
				.thenApply(positions -> {
					log.info("Calculating performance based on positions = {}", positions);
					if (positions.isEmpty()) {
						return BigDecimal.ZERO;
					}
					return positions
							.stream()
							.map(this::calculatePositionPerformance)
							.reduce(BigDecimal::add)
							.orElse(BigDecimal.ZERO)
							.divide(BigDecimal.valueOf(positions.size()), MATH_CONTEXT);
				});
	}

	private BigDecimal calculatePositionPerformance(Position v) {
		return v.getCurrentPrice().divide(v.getPurchasePrice(), MATH_CONTEXT);
	}

}
