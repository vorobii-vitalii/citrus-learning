package org.citrus.learn.paymentsservice.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

	@Value(value = "${spring.kafka.security.protocol:}")
	private String kafkaSecurityProtocol;

	@Value(value = "${spring.kafka.sasl.mechanism:}")
	private String saslMechanism;

	@Value(value = "${spring.kafka.sasl.jaas.config:}")
	private String saslJaasConfig;

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		ifSet(v -> configs.put("security.protocol", v), kafkaSecurityProtocol);
		ifSet(v -> configs.put("sasl.mechanism", v), saslMechanism);
		ifSet(v -> configs.put("sasl.jaas.config", v), saslJaasConfig);
		return new KafkaAdmin(configs);
	}

	private void ifSet(Consumer<String> consumer, String v) {
		if (v != null && !v.isEmpty()) {
			consumer.accept(v);
		}
	}

	@Bean
	public NewTopic paymentRequestsNewTopic(@Value("${payment.requests.topic}") String paymentRequestsTopic) {
		return new NewTopic(paymentRequestsTopic, 3, (short) 1);
	}
}
