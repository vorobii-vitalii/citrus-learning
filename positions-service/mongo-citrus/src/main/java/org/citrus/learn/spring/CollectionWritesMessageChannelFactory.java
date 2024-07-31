package org.citrus.learn.spring;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

public class CollectionWritesMessageChannelFactory {
	protected static final String WRITES_SUFFIX = "-writes";

	public MessageChannel createMessageChannelForCollection(String collectionName) {
		var messageChannel = new DirectChannel();
		messageChannel.setBeanName(collectionName + WRITES_SUFFIX);
		return messageChannel;
	}

}
