package org.citrus.learn.positionsservice.dao.mongo;

import static org.citrus.learn.positionsservice.dao.mongo.MongoClientPositionsRepository.CLIENT_ID;
import static org.mockito.Mockito.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.domain.ClientPositionCollectionObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveFindOperation;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TestMongoClientPositionsRepository {
	private static final String CLIENT_1 = "1";
	private static final String CLIENT_2 = "2";
	private static final Set<String> CLIENT_IDS = Set.of(CLIENT_1, CLIENT_2);

	@Mock
	ReactiveMongoTemplate mongoTemplate;

	@InjectMocks
	MongoClientPositionsRepository mongoClientPositionsRepository;

	@Mock
	ReactiveFindOperation.ReactiveFind<ClientPositionCollectionObject> clientPositionCollectionObjectReactiveFind;

	@Mock
	ReactiveFindOperation.TerminatingFind<ClientPositionCollectionObject> clientPositionCollectionObjectTerminatingFind;

	@Test
	void shouldCorrectlyCreatePositionsReadQueryToMongo() {
		when(mongoTemplate.query(ClientPositionCollectionObject.class)).thenReturn(clientPositionCollectionObjectReactiveFind);
		when(clientPositionCollectionObjectReactiveFind.matching(where(CLIENT_ID).in(CLIENT_IDS)))
				.thenReturn(clientPositionCollectionObjectTerminatingFind);
		when(clientPositionCollectionObjectTerminatingFind.all()).thenReturn(Flux.just(
				ClientPositionCollectionObject.builder()
						.clientId(CLIENT_1)
						.positionId(ObjectId.get())
						.purchasePrice(BigDecimal.ONE)
						.quantity(BigDecimal.valueOf(2L))
						.symbol("ABBN")
						.build(),
				ClientPositionCollectionObject.builder()
						.clientId(CLIENT_1)
						.positionId(ObjectId.get())
						.purchasePrice(BigDecimal.TEN)
						.quantity(BigDecimal.ONE)
						.symbol("XBT/USD")
						.build()
		));
		StepVerifier.create(mongoClientPositionsRepository.findPositionsForClients(CLIENT_IDS))
				.expectNext(Map.of(
						CLIENT_1, List.of(
								Position.newBuilder()
										.purchasePrice(BigDecimal.ONE)
										.quantity(BigDecimal.valueOf(2L))
										.symbol("ABBN")
										.build(),
								Position.newBuilder()
										.purchasePrice(BigDecimal.TEN)
										.quantity(BigDecimal.ONE)
										.symbol("XBT/USD")
										.build()
						),
						CLIENT_2, List.of()
				))
				.expectComplete()
				.log()
				.verify();
	}
}
