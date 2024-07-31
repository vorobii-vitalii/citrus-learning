package org.citrus.learn.integration_tests.config;

import org.citrus.learn.integration_tests.BaseIntegrationTest;
import org.citrus.learn.spring.BaseMongoEndpointConfiguration;
import org.citrus.learn.spring.MongoEndpointFactory;
import org.citrusframework.endpoint.Endpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
@EnableIntegration
@Import(BaseMongoEndpointConfiguration.class)
public class MongoEndpointConfig {

	private static final String MONGO_DATABASE = "test";
	private static final String POSITIONS_COLLECTION = "positions";

	@Bean
	MongoClient mongoClient() {
		return MongoClients.create(BaseIntegrationTest.MONGO.getConnectionString());
	}

	@Bean
	@Qualifier("positionsCollectionMongoEndpoint")
	Endpoint positionsCollectionMongoEndpoint(
			MongoEndpointFactory mongoEndpointFactory,
			MongoClient mongoClient
	) {
		return mongoEndpointFactory.create(mongoClient, MONGO_DATABASE, POSITIONS_COLLECTION);
	}

}
