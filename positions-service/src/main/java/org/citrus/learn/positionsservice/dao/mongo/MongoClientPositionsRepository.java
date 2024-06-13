package org.citrus.learn.positionsservice.dao.mongo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.dao.ClientPositionsRepository;
import org.citrus.learn.positionsservice.domain.ClientPositionCollectionObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MongoClientPositionsRepository implements ClientPositionsRepository {
	protected static final String CLIENT_ID = "clientId";

	private final ReactiveMongoTemplate mongoTemplate;

	@Override
	public Mono<Map<String, List<Position>>> findPositionsForClients(Set<String> clientIds) {
		return Mono.defer(() -> fetchPositionsByClientId(clientIds)
				.collectMap(Pair::getFirst, Pair::getSecond)
				.map(map -> ensureNotNullPositionsForAllClients(clientIds, map)));
	}

	private @NotNull HashMap<String, List<Position>> ensureNotNullPositionsForAllClients(
			Set<String> clientIds,
			Map<String, List<Position>> positionsByClientId
	) {
		var positionsByClientCopy = new HashMap<>(positionsByClientId);
		for (String clientId : clientIds) {
			if (!positionsByClientId.containsKey(clientId)) {
				positionsByClientCopy.put(clientId, new ArrayList<>());
			}
		}
		return positionsByClientCopy;
	}

	private @NotNull Flux<Pair<String, List<Position>>> fetchPositionsByClientId(Set<String> clientIds) {
		return mongoTemplate.query(ClientPositionCollectionObject.class)
				.matching(where(CLIENT_ID).in(clientIds))
				.all()
				.groupBy(ClientPositionCollectionObject::getClientId)
				.flatMap(flux -> flux.map(this::fromCollectionObject)
						.buffer()
						.switchIfEmpty(Flux.just(List.of()))
						.last()
						.map(v -> Pair.of(flux.key(), v)));
	}

	private Position fromCollectionObject(ClientPositionCollectionObject object) {
		return Position.newBuilder()
				.purchasePrice(object.getPurchasePrice())
				.quantity(object.getQuantity())
				.symbol(object.getSymbol())
				.build();
	}

}
