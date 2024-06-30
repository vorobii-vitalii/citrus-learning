package org.citrus.learn.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.citrus.learn.spring.CollectionWritesMessageChannelFactory.WRITES_SUFFIX;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationObjectSupport;

class TestCollectionWritesMessageChannelFactory {
	private static final String COLLECTION_NAME = "collection";

	CollectionWritesMessageChannelFactory collectionWritesMessageChannelFactory = new CollectionWritesMessageChannelFactory();

	@Test
	void shouldCorrectlyCreateMessageChannelForCollection() {
		var actualMessageChannel = collectionWritesMessageChannelFactory.createMessageChannelForCollection(COLLECTION_NAME);
		assertThat(actualMessageChannel)
				.asInstanceOf(InstanceOfAssertFactories.type(DirectChannel.class))
				.extracting(IntegrationObjectSupport::getBeanName)
				.isEqualTo(COLLECTION_NAME + WRITES_SUFFIX);
	}
}