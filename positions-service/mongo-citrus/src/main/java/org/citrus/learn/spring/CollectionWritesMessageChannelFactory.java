package org.citrus.learn.spring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

public class CollectionWritesMessageChannelFactory {
	private static final String WRITES_SUFFIX = "-writes";

	private final Map<String, MessageChannel> messageChannelByCollection = new ConcurrentHashMap<>();

	public MessageChannel createMessageChannelForCollection(String collectionName) {
		return messageChannelByCollection.computeIfAbsent(collectionName, s -> {
			var messageChannel = new DirectChannel();
			messageChannel.setBeanName(collectionName + WRITES_SUFFIX);
			return messageChannel;
		});
	}

}
