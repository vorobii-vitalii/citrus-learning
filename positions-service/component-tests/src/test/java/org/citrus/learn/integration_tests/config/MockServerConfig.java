package org.citrus.learn.integration_tests.config;

import org.citrus.learn.integration_tests.BaseIntegrationTest;
import org.citrus.learn.integration_tests.actions.MockQuoteServiceActions;
import org.mockserver.client.MockServerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockServerConfig {

	@Bean
	MockServerClient mockServerClient() {
		return new MockServerClient(BaseIntegrationTest.MOCK_SERVER.getHost(), BaseIntegrationTest.MOCK_SERVER.getServerPort());
	}

	@Bean
	MockQuoteServiceActions mockQuoteServiceActions(MockServerClient mockServerClient) {
		return new MockQuoteServiceActions(mockServerClient);
	}

}
