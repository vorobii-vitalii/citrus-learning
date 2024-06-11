package org.citrus.learn.positionsservice.datafetchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.dataloader.DataLoader;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

@DgsComponent
@Slf4j
public class PositionDetailsDataFetcher {

	@DgsData(field = DgsConstants.POSITIONDETAILS.Positions, parentType = DgsConstants.POSITIONDETAILS.TYPE_NAME)
	public List<Position> fetchPositions(DataFetchingEnvironment environment) {
		// TODO: Load positions without current prices from DB
		ClientId clientId = environment.getArgument("clientId");
		log.info("Fetching positions of client = {}", clientId);
		return List.of(
				Position.newBuilder()
						.id("1")
						.symbol("XBT/USD")
						.purchaseDate("2024/06/01")
						.purchasePrice(123D)
						.quantity(1)
						.build(),
				Position.newBuilder()
						.id("2")
						.symbol("RPL/USD")
						.purchaseDate("2024/06/03")
						.purchasePrice(99D)
						.quantity(5)
						.build()
		);
	}

	@DgsData(field = DgsConstants.POSITION.CurrentPrice, parentType = DgsConstants.POSITION.TYPE_NAME)
	public CompletableFuture<BigDecimal> fetchCurrentPrice(DataFetchingEnvironment environment) {
		DataLoader<String, BigDecimal> pricesLoader = Objects.requireNonNull(environment.getDataLoader("prices"));
		Position position = Objects.requireNonNull(environment.getSource());
		log.info("Going to fetch price for position = {}", position);
		return pricesLoader.load(position.getSymbol());
	}

}
