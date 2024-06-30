package org.citrus.learn.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.context.IntegrationFlowContext;

@EnableIntegration
public class BaseMongoEndpointConfiguration {

	@Bean
	IntegrationFlowRegistrar integrationFlowRegistrar(IntegrationFlowContext integrationFlowContext) {
		return new IntegrationFlowRegistrar(integrationFlowContext);
	}

	@Bean
	CollectionWritesMessageChannelFactory collectionWritesMessageChannelFactory() {
		return new CollectionWritesMessageChannelFactory();
	}

	@Bean
	MongoEndpointFactory mongoEndpointFactory(
			IntegrationFlowRegistrar integrationFlowRegistrar,
			CollectionWritesMessageChannelFactory collectionWritesMessageChannelFactory
	) {
		return new MongoEndpointFactory(integrationFlowRegistrar, collectionWritesMessageChannelFactory);
	}

}
