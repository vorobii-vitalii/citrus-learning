package org.citrus.learn.integration_tests.config;

import static org.citrus.learn.integration_tests.BaseIntegrationTest.GRAPHQL_SERVICE_PORT;
import static org.citrus.learn.integration_tests.BaseIntegrationTest.POSITIONS_SERVICE;
import static org.citrusframework.http.endpoint.builder.HttpEndpoints.http;

import org.citrusframework.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PositionsGraphQlServiceEndpointConfig {

	@Qualifier("positionsGraphQLServiceEndpoint")
	@Bean
	public HttpClient positionsGraphQLServiceEndpoint() {
		return http()
				.client()
				.requestUrl("http://localhost:%s/".formatted(POSITIONS_SERVICE.getMappedPort(GRAPHQL_SERVICE_PORT)))
				.build();
	}

}
