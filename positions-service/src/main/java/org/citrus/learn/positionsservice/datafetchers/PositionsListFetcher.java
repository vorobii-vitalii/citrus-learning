package org.citrus.learn.positionsservice.datafetchers;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.dataloader.DataLoader;
import org.jetbrains.annotations.NotNull;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;

@DgsComponent
@Slf4j
public class PositionsListFetcher {

	@DgsData(field = DgsConstants.POSITIONDETAILS.Positions, parentType = DgsConstants.POSITIONDETAILS.TYPE_NAME)
	public CompletableFuture<List<Position>> fetchPositions(DataFetchingEnvironment environment) {
		PositionDetailsLoadContext positionDetailsLoadContext = Objects.requireNonNull(environment.getLocalContext());
		log.info("Going to load list of positions. Context = {}", positionDetailsLoadContext);
		DataLoader<String, List<Position>> positionLoader = getPositionsDataLoader(environment);
		return positionLoader.load(positionDetailsLoadContext.clientId().getId(), positionDetailsLoadContext);
	}

	private @NotNull DataLoader<String, List<Position>> getPositionsDataLoader(DataFetchingEnvironment environment) {
		return Objects.requireNonNull(environment.getDataLoader("positions"));
	}

}
