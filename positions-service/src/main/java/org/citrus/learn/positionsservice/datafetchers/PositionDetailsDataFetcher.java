package org.citrus.learn.positionsservice.datafetchers;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.codegen.types.PositionDetails;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.predicate.RequestPricesPredicate;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@DgsComponent
@Slf4j
@RequiredArgsConstructor
public class PositionDetailsDataFetcher {
	private final RequestPricesPredicate requestPricesPredicate;

	@DgsQuery(field = DgsConstants.QUERY.PositionDetails)
	public DataFetcherResult<PositionDetails> getPositionDetails(
			DataFetchingEnvironment dataFetchingEnvironment,
			@InputArgument(name = DgsConstants.QUERY.POSITIONDETAILS_INPUT_ARGUMENT.ClientId) ClientId clientId
	) {
		log.info("Fetching positions of client = {} position details", clientId);
		var positionDetailsLoadContext = PositionDetailsLoadContext.builder()
				.fetchPositionsCurrentPrices(requestPricesPredicate.shouldRequestPrices(dataFetchingEnvironment))
				.clientId(clientId)
				.build();
		log.info("Calculated position details load context = {}", positionDetailsLoadContext);
		return DataFetcherResult.<PositionDetails> newResult()
				.data(new PositionDetails())
				.localContext(positionDetailsLoadContext)
				.build();
	}

}
