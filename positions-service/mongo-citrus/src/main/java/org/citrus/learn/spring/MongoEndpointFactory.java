package org.citrus.learn.spring;

import org.citrusframework.channel.ChannelEndpointBuilder;
import org.citrusframework.endpoint.Endpoint;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mongodb.dsl.MongoDb;
import org.springframework.integration.mongodb.dsl.ReactiveMongoDbMessageHandlerSpec;

import com.mongodb.reactivestreams.client.MongoClient;

public class MongoEndpointFactory {
	private final IntegrationFlowRegistrar integrationFlowRegistrar;
	private final CollectionWritesMessageChannelFactory collectionWritesMessageChannelFactory;

	public MongoEndpointFactory(
			IntegrationFlowRegistrar integrationFlowRegistrar,
			CollectionWritesMessageChannelFactory collectionWritesMessageChannelFactory
	) {
		this.integrationFlowRegistrar = integrationFlowRegistrar;
		this.collectionWritesMessageChannelFactory = collectionWritesMessageChannelFactory;
	}

	public Endpoint create(MongoClient mongoClient, String database, String collectionName) {
		var writesChannel = collectionWritesMessageChannelFactory.createMessageChannelForCollection(collectionName);
		var integrationFlow = IntegrationFlow.from(writesChannel)
				.log()
				.handle(createMongoMessageHandler(mongoClient, database, collectionName))
				.get();
		integrationFlowRegistrar.register(integrationFlow);
		return new ChannelEndpointBuilder().channel(writesChannel).build();
	}

	private ReactiveMongoDbMessageHandlerSpec createMongoMessageHandler(MongoClient mongoClient, String database, String collection) {
		var mongoDbFactory = new SimpleReactiveMongoDatabaseFactory(mongoClient, database);
		return MongoDb.reactiveOutboundChannelAdapter(mongoDbFactory).collectionName(collection);
	}

}
