package org.citrus.learn.integration_tests.actions;

import static org.citrusframework.http.actions.HttpActionBuilder.http;

import java.util.Map;

import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.http.actions.HttpClientResponseActionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.google.gson.Gson;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;

public class PositionsGraphQLServiceActions {
	private static final Logger LOGGER = LoggerFactory.getLogger(PositionsGraphQLServiceActions.class);

	private final String positionsServiceURI;
	private final Gson gson = new Gson();

	public PositionsGraphQLServiceActions(String positionsServiceURI) {
		LOGGER.info("Positions service URI = {}", positionsServiceURI);
		this.positionsServiceURI = positionsServiceURI;
	}

	public AbstractTestAction performGraphQLRequest(GraphQLQueryRequest request) {
		var serializedRequest = request.serialize();
		LOGGER.info("Sending GraphQL request = {}", serializedRequest);
		var wrapper = new GraphQLRequestWrapper(serializedRequest, Map.of());
		return http()
				.client(positionsServiceURI)
				.send()
				.post("/graphql")
				.message()
				.header("Content-Type", "application/json")
				.body(gson.toJson(wrapper))
				.build();
	}

	public HttpClientResponseActionBuilder.HttpMessageBuilderSupport expectStatusCodeAndBody(HttpStatus expectedErrorCode, String expectedBody) {
		return http()
				.client(positionsServiceURI)
				.receive()
				.response(expectedErrorCode)
				.message()
				.body(expectedBody);
	}

	public record GraphQLRequestWrapper(String query, Map<String, String> variables) {
	}

}
