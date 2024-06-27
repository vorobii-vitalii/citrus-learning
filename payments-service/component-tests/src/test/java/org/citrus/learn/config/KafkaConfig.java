package org.citrus.learn.config;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.citrus.learn.BaseIntegrationTest.KAFKA;
import static org.citrus.learn.utils.Constants.PAYMENT_REQUESTS_TOPIC;
import static org.citrus.learn.utils.Constants.PAYMENT_RESPONSES_TOPIC;

import java.util.Arrays;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.endpoint.KafkaEndpointBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class KafkaConfig {
	private static final int NUM_PARTITIONS = 1;
	private static final short REPLICATION_FACTOR = (short) 1;

	@PostConstruct
	public void createNecessaryTopics() {
		createTopics(PAYMENT_REQUESTS_TOPIC, PAYMENT_RESPONSES_TOPIC);
	}

	private void createTopics(String... topics) {
		var newTopics = Arrays.stream(topics).map(topic -> new NewTopic(topic, NUM_PARTITIONS, REPLICATION_FACTOR)).toList();
		try (var admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
			admin.createTopics(newTopics);
		}
	}

	@Bean("paymentRequestsTopicEndpoint")
	KafkaEndpoint paymentRequestsTopicEndpoint() {
		return new KafkaEndpointBuilder().server(KAFKA.getBootstrapServers()).topic(PAYMENT_REQUESTS_TOPIC).build();
	}

	@Bean("paymentResponsesTopicEndpoint")
	KafkaEndpoint paymentResponsesTopicEndpoint() {
		return new KafkaEndpointBuilder().server(KAFKA.getBootstrapServers()).topic(PAYMENT_RESPONSES_TOPIC).build();
	}

}
