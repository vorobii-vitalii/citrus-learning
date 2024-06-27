package org.citrus.learn.integration_tests;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.citrus.learn.integration_tests.actions.MockQuoteServiceActions;
import org.citrus.learn.integration_tests.domain.ClientPositionCollectionObject;
import org.citrus.learn.positionsservice.codegen.client.PositionDetailsGraphQLQuery;
import org.citrus.learn.positionsservice.codegen.client.PositionDetailsProjectionRoot;
import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusEndpoint;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageBuilder;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.citrusframework.message.DefaultMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.gson.Gson;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

import lombok.extern.slf4j.Slf4j;

@Testcontainers
@CitrusSpringSupport
@ContextConfiguration(classes = CitrusSpringConfig.class)
@Slf4j
public class PositionServiceIntegrationTest extends BaseIntegrationTest {
	private final Gson gson = new Gson();

	@CitrusEndpoint(name = "positionsCollectionMongoEndpoint")
	Endpoint positionsCollectionMongoEndpoint;

	@CitrusEndpoint(name = "positionsGraphQLServiceEndpoint")
	Endpoint positionsGraphQLServiceEndpoint;

	MockServerClient mockServerClient;
	MockQuoteServiceActions mockQuoteServiceActions;

	@BeforeEach
	void init() {
		mockServerClient = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getServerPort());
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

		actions.$(send(positionsCollectionMongoEndpoint)
				.message(new DefaultMessage()
						.setPayload(ClientPositionCollectionObject.builder()
								.clientId(clientId)
								.positionId(ObjectId.get())
								.purchasePrice(BigDecimal.ONE)
								.quantity(BigDecimal.ONE)
								.symbol("XBT/USD")
								.build()
						)));
		actions.$(send(positionsGraphQLServiceEndpoint).message(new HttpMessageBuilder(
				new HttpMessage(gson.toJson(new GraphQLRequestWrapper(new GraphQLQueryRequest(
						new PositionDetailsGraphQLQuery.Builder()
								.clientId(ClientId.newBuilder().id(clientId).build())
								.build(),
						new PositionDetailsProjectionRoot<>()
								.positions()
								.purchasePrice()
								.quantity()
								.symbol()
				).serialize(), Map.of())))
						.method(HttpMethod.POST)
						.path("/graphql")
						.contentType("application/json")
		)));
		actions.$(receive(positionsGraphQLServiceEndpoint).message().body("""
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

		actions.$(send(positionsCollectionMongoEndpoint)
				.message(new DefaultMessage()
						.setPayload(ClientPositionCollectionObject.builder()
								.clientId(clientId)
								.positionId(ObjectId.get())
								.purchasePrice(BigDecimal.ONE)
								.quantity(BigDecimal.ONE)
								.symbol("XBT/USD")
								.build()
						)));
		actions.$(send(positionsGraphQLServiceEndpoint).message(new HttpMessageBuilder(
				new HttpMessage(gson.toJson(new GraphQLRequestWrapper(
						new GraphQLQueryRequest(
								new PositionDetailsGraphQLQuery.Builder()
										.clientId(ClientId.newBuilder().id(clientId).build())
										.build(),
								new PositionDetailsProjectionRoot<>()
										.positions()
										.purchasePrice()
										.quantity()
										.currentPrice()
										.symbol()
						).serialize(),
						Map.of()
				)))
						.method(HttpMethod.POST)
						.path("/graphql")
						.contentType("application/json")
		)));
		actions.$(receive(positionsGraphQLServiceEndpoint).message().body("""
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

		actions.$(send(positionsCollectionMongoEndpoint).message(new DefaultMessage()
				.setPayload(ClientPositionCollectionObject.builder()
						.clientId(clientId)
						.positionId(ObjectId.get())
						.purchasePrice(BigDecimal.ONE)
						.quantity(BigDecimal.ONE)
						.symbol("XBT/USD")
						.build()
				)));

		actions.$(send(positionsGraphQLServiceEndpoint).message(new HttpMessageBuilder(
				new HttpMessage(gson.toJson(new GraphQLRequestWrapper(
						new GraphQLQueryRequest(
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
						).serialize(),
						Map.of()
				)))
						.method(HttpMethod.POST)
						.path("/graphql")
						.contentType("application/json")
		)));

		actions.$(receive(positionsGraphQLServiceEndpoint).message().body("""
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
