package org.citrus.learn.config;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.citrus.learn.BaseIntegrationTest.KAFKA;
import static org.citrus.learn.utils.Constants.PAYMENT_REQUESTS;
import static org.citrus.learn.utils.Constants.PAYMENT_RESPONSE;

import java.util.Arrays;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class KafkaConfig {
	private static final int NUM_PARTITIONS = 1;
	private static final short REPLICATION_FACTOR = (short) 1;

	private static void createTopics(String... topics) {
		var newTopics = Arrays.stream(topics).map(topic -> new NewTopic(topic, NUM_PARTITIONS, REPLICATION_FACTOR)).toList();
		try (var admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
			admin.createTopics(newTopics);
		}
	}

	@PostConstruct
	public void createNecessaryTopics() {
		createTopics(PAYMENT_REQUESTS, PAYMENT_RESPONSE);
	}

}
