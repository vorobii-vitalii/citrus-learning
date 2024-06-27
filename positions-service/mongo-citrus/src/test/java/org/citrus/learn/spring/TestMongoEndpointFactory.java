package org.citrus.learn.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.citrusframework.channel.ChannelEndpoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.messaging.MessageChannel;

import com.mongodb.reactivestreams.client.MongoClient;

class TestMongoEndpointFactory {
	private static final String COLLECTION_NAME = "coll";
	private static final String DATABASE = "DB";

	IntegrationFlowRegistrar integrationFlowRegistrar = mock(IntegrationFlowRegistrar.class);

	CollectionWritesMessageChannelFactory collectionWritesMessageChannelFactory = mock(CollectionWritesMessageChannelFactory.class);

	MongoEndpointFactory mongoEndpointFactory = new MongoEndpointFactory(integrationFlowRegistrar, collectionWritesMessageChannelFactory);

	MongoClient mongoClient = mock(MongoClient.class);

	MessageChannel messageChannel = mock(MessageChannel.class);

	ArgumentCaptor<StandardIntegrationFlow> standardIntegrationFlowArgumentCaptor = ArgumentCaptor.forClass(StandardIntegrationFlow.class);

	@Test
	void shouldCorrectlyCreateMongoEndpoint() {
		when(collectionWritesMessageChannelFactory.createMessageChannelForCollection(COLLECTION_NAME))
				.thenReturn(messageChannel);
		assertThat(mongoEndpointFactory.create(mongoClient, DATABASE, COLLECTION_NAME))
				.asInstanceOf(InstanceOfAssertFactories.type(ChannelEndpoint.class))
				.extracting(v -> v.getEndpointConfiguration().getChannel())
				.isEqualTo(messageChannel);
		verify(integrationFlowRegistrar).register(standardIntegrationFlowArgumentCaptor.capture());
		var integrationFlow = standardIntegrationFlowArgumentCaptor.getValue();
		assertThat(integrationFlow.getInputChannel()).isEqualTo(messageChannel);
	}
}
