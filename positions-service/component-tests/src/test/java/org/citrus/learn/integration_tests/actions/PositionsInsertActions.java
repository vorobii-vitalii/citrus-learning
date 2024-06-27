package org.citrus.learn.integration_tests.actions;

import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.Sequence.Builder.sequential;

import java.util.List;

import org.citrus.learn.integration_tests.domain.ClientPositionCollectionObject;
import org.citrusframework.actions.SendMessageAction;
import org.citrusframework.container.Sequence;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.message.DefaultMessage;
import org.springframework.context.ApplicationContext;

public class PositionsInsertActions {
	private final Endpoint positionsEndpoint;

	public PositionsInsertActions(ApplicationContext context) {
		this.positionsEndpoint = context.getBean("positionsCollectionMongoEndpoint", Endpoint.class);
	}

	public Sequence.Builder insertPositions(List<ClientPositionCollectionObject> positionsToStore) {
		return sequential().actions(positionsToStore.stream()
				.map(this::createPositionInsertAction)
				.toArray(SendMessageAction.SendMessageActionBuilderSupport[]::new));
	}

	private SendMessageAction.SendMessageActionBuilderSupport createPositionInsertAction(ClientPositionCollectionObject v) {
		return send(positionsEndpoint).message(new DefaultMessage().setPayload(v));
	}

}
