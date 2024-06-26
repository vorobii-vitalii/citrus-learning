package org.citrus.learn.integration_tests;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.citrus.learn.integration_tests.actions.MockQuoteServiceActions;
import org.citrus.learn.integration_tests.actions.PositionsGraphQLServiceActions;
import org.citrus.learn.integration_tests.actions.PositionsInsertActions;
import org.citrus.learn.integration_tests.domain.ClientPositionCollectionObject;
import org.citrus.learn.positionsservice.codegen.client.PositionDetailsGraphQLQuery;
import org.citrus.learn.positionsservice.codegen.client.PositionDetailsProjectionRoot;
import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

import lombok.extern.slf4j.Slf4j;

@Testcontainers
@CitrusSpringSupport
@ContextConfiguration(classes = CitrusSpringConfig.class)
@Slf4j
public class PositionServiceIntegrationTest extends BaseIntegrationTest {
	private static final String MONGO_DATABASE = "test";
	private static final String POSITIONS_COLLECTION = "positions";

	@Autowired
	ApplicationContext applicationContext;
	MockServerClient mockServerClient;
	PositionsInsertActions positionsInsertActions;
	PositionsGraphQLServiceActions positionsGraphQLServiceActions;
	MockQuoteServiceActions mockQuoteServiceActions;

	@BeforeEach
	void init() {
		mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getServerPort());
		positionsInsertActions =
				new PositionsInsertActions(applicationContext, MONGO.getConnectionString(), MONGO_DATABASE, POSITIONS_COLLECTION);
		positionsGraphQLServiceActions = new PositionsGraphQLServiceActions("http://localhost:%s/".formatted(
				POSITIONS_SERVICE.getMappedPort(GRAPHQL_SERVICE_PORT)
		));
		mockQuoteServiceActions = new MockQuoteServiceActions(mockServerClient);
	}

	@AfterEach
	void clearStubs() {
		mockServerClient.reset();
	}

	@Test
	@CitrusTest
	void shouldNotCallQuoteServiceIfCurrentPricesAndPerformanceNotRequested(@CitrusResource TestActionRunner actions) {
		var clientId = UUID.randomUUID().toString();

		actions.$(positionsInsertActions.insertPositions(
				List.of(
						ClientPositionCollectionObject.builder()
								.clientId(clientId)
								.positionId(ObjectId.get())
								.purchasePrice(BigDecimal.ONE)
								.quantity(BigDecimal.ONE)
								.symbol("XBT/USD")
								.build()
				)));
		actions.$(positionsGraphQLServiceActions.performGraphQLRequest(new GraphQLQueryRequest(
				new PositionDetailsGraphQLQuery.Builder()
						.clientId(ClientId.newBuilder().id(clientId).build())
						.build(),
				new PositionDetailsProjectionRoot<>()
						.positions()
						.purchasePrice()
						.quantity()
						.symbol()
		)));
		actions.$(positionsGraphQLServiceActions.expectStatusCodeAndBody(HttpStatus.OK, """
				{
				    "data": {
				        "positionDetails": {
				            "positions": [
				                {
				                    "quantity": 1,
				                    "purchasePrice": 1,
				                    "symbol": "XBT/USD"
				                }
				            ]
				        }
				    }
				}
				"""));
		actions.$(mockQuoteServiceActions.verifyNotCalled());
	}

	@Test
	@CitrusTest
	void shouldCallQuoteServiceToFetchPricesIfCurrentPositionPricesAreRequested(@CitrusResource TestActionRunner actions) {
		var clientId = UUID.randomUUID().toString();

		actions.$(mockQuoteServiceActions.mockPrices(Map.of("XBT/USD", BigDecimal.TEN)));

		actions.$(positionsInsertActions.insertPositions(
				List.of(
						ClientPositionCollectionObject.builder()
								.clientId(clientId)
								.positionId(ObjectId.get())
								.purchasePrice(BigDecimal.ONE)
								.quantity(BigDecimal.ONE)
								.symbol("XBT/USD")
								.build()
				)));
		actions.$(positionsGraphQLServiceActions.performGraphQLRequest(new GraphQLQueryRequest(
				new PositionDetailsGraphQLQuery.Builder()
						.clientId(ClientId.newBuilder().id(clientId).build())
						.build(),
				new PositionDetailsProjectionRoot<>()
						.positions()
						.purchasePrice()
						.quantity()
						.currentPrice()
						.symbol()
		)));
		actions.$(positionsGraphQLServiceActions.expectStatusCodeAndBody(HttpStatus.OK, """
				{
				    "data": {
				        "positionDetails": {
				            "positions": [
				                {
				                    "quantity": 1,
				                    "purchasePrice": 1,
				                    "currentPrice": 10,
				                    "symbol": "XBT/USD"
				                }
				            ]
				        }
				    }
				}
				"""));
	}

	@Test
	@CitrusTest
	void shouldCallQuoteServiceToFetchPricesIfPerformanceRequested(@CitrusResource TestActionRunner actions) {
		var clientId = UUID.randomUUID().toString();

		actions.$(mockQuoteServiceActions.mockPrices(Map.of("XBT/USD", BigDecimal.TEN)));

		actions.$(positionsInsertActions.insertPositions(
				List.of(
						ClientPositionCollectionObject.builder()
								.clientId(clientId)
								.positionId(ObjectId.get())
								.purchasePrice(BigDecimal.ONE)
								.quantity(BigDecimal.ONE)
								.symbol("XBT/USD")
								.build()
				)));
		actions.$(positionsGraphQLServiceActions.performGraphQLRequest(new GraphQLQueryRequest(
				new PositionDetailsGraphQLQuery.Builder()
						.clientId(ClientId.newBuilder().id(clientId).build())
						.build(),
				new PositionDetailsProjectionRoot<>()
						.positions()
						.purchasePrice()
						.quantity()
						.symbol()
						.getParent()
						.performance()
		)));
		actions.$(positionsGraphQLServiceActions.expectStatusCodeAndBody(HttpStatus.OK, """
				{
				    "data": {
				        "positionDetails": {
				            "positions": [
				                {
				                    "quantity": 1,
				                    "purchasePrice": 1,
				                    "symbol": "XBT/USD"
				                }
				            ],
				            "performance": 9
				        }
				    }
				}
				"""));
	}

}
