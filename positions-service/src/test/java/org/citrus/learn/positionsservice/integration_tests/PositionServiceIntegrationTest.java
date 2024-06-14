package org.citrus.learn.positionsservice.integration_tests;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.citrus.learn.positionsservice.codegen.client.PositionDetailsGraphQLQuery;
import org.citrus.learn.positionsservice.codegen.client.PositionDetailsProjectionRoot;
import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.domain.ClientPositionCollectionObject;
import org.citrus.learn.positionsservice.integration_tests.actions.MockQuoteServiceActions;
import org.citrus.learn.positionsservice.integration_tests.actions.PositionsGraphQLServiceActions;
import org.citrus.learn.positionsservice.integration_tests.actions.PositionsInsertActions;
import org.citrus.learn.positionsservice.integration_tests.utils.JavaOptionsCreator;
import org.citrus.learn.positionsservice.integration_tests.utils.LocalJavaContainer;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

import groovy.util.logging.Slf4j;

@Testcontainers
@CitrusSpringSupport
@ContextConfiguration(classes = CitrusSpringConfig.class)
@Slf4j
public class PositionServiceIntegrationTest {
	private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
			.parse("mockserver/mockserver")
			.withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
	private static final String MONGO_DATABASE = "test";
	private static final String POSITIONS_COLLECTION = "positions";
	private static final Logger LOGGER = LoggerFactory.getLogger(PositionServiceIntegrationTest.class);
	private static final int GRAPHQL_SERVICE_PORT = 8080;

	static Network network = Network.newNetwork();
	@Container
	static public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)
			.withNetwork(network)
			.withNetworkAliases("mockserver")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mock-server"));
	@Container
	static MongoDBContainer mongo = new MongoDBContainer()
			.withNetwork(network)
			.withNetworkAliases("mongo")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mongo"));
	@Container
	static GenericContainer<?> positionsService = new LocalJavaContainer<>()
			.withNetwork(network)
			.dependsOn(mongo, mockServer)
			.withEnv(Map.of(
					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
							"finn.hub.api.url", "http://mockserver:1080/",
							"spring.data.mongodb.uri", "mongodb://mongo:27017/test"
					))
			))
			.withExposedPorts(GRAPHQL_SERVICE_PORT)
			.waitingFor(Wait.forListeningPorts(GRAPHQL_SERVICE_PORT))
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("payment-service"));
	@Autowired
	ApplicationContext applicationContext;
	MockServerClient mockServerClient;
	PositionsInsertActions positionsInsertActions;
	PositionsGraphQLServiceActions positionsGraphQLServiceActions;
	MockQuoteServiceActions mockQuoteServiceActions;

	@BeforeEach
	void init() {
		mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
		positionsInsertActions =
				new PositionsInsertActions(applicationContext, mongo.getConnectionString(), MONGO_DATABASE, POSITIONS_COLLECTION);
		positionsGraphQLServiceActions = new PositionsGraphQLServiceActions("http://localhost:%s/".formatted(
				positionsService.getMappedPort(GRAPHQL_SERVICE_PORT)
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
