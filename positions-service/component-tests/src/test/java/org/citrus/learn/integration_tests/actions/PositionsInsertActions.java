package org.citrus.learn.integration_tests.actions;

import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.Sequence.Builder.sequential;

import java.util.List;

import org.citrus.learn.integration_tests.domain.ClientPositionCollectionObject;
import org.citrusframework.actions.SendMessageAction;
import org.citrusframework.channel.ChannelEndpoint;
import org.citrusframework.channel.ChannelEndpointBuilder;
import org.citrusframework.container.Sequence;
import org.citrusframework.message.DefaultMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.mongodb.dsl.MongoDb;
import org.springframework.integration.mongodb.dsl.ReactiveMongoDbMessageHandlerSpec;
import org.springframework.messaging.MessageChannel;

import com.mongodb.reactivestreams.client.MongoClients;

public class PositionsInsertActions {
	private final ChannelEndpoint mongoUpdateEndpoint;

	public PositionsInsertActions(ApplicationContext context, String mongoConnectionURL, String databaseName, String collectionName) {
		var integrationFlowContext = context.getBean(IntegrationFlowContext.class);
		var mongoWritesChannel = context.getBean("mongoWritesChannel", MessageChannel.class);
		var integrationFlow = IntegrationFlow.from(mongoWritesChannel)
				.log()
				.handle(createMongoMessageHandler(mongoConnectionURL, databaseName, collectionName))
				.get();
		integrationFlowContext.registration(integrationFlow).register().start();
		mongoUpdateEndpoint = new ChannelEndpointBuilder().channel(mongoWritesChannel).build();
	}

	public Sequence.Builder insertPositions(List<ClientPositionCollectionObject> positionsToStore) {
		return sequential().actions(positionsToStore.stream()
				.map(this::createPositionInsertAction)
				.toArray(SendMessageAction.SendMessageActionBuilderSupport[]::new));
	}

	private SendMessageAction.SendMessageActionBuilderSupport createPositionInsertAction(ClientPositionCollectionObject v) {
		return send(mongoUpdateEndpoint).message(new DefaultMessage().setPayload(v));
	}

	private ReactiveMongoDbMessageHandlerSpec createMongoMessageHandler(
			String mongoConnectionURL,
			String databaseName,
			String collectionName
	) {
		var mongoDbFactory = new SimpleReactiveMongoDatabaseFactory(MongoClients.create(mongoConnectionURL), databaseName);
		return MongoDb.reactiveOutboundChannelAdapter(mongoDbFactory).collectionName(collectionName);
	}

}
