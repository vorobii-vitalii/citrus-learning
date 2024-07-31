package org.citrus.learn.paymentsservice.config;

import java.util.HashMap;
import java.util.Map;

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

	@Value(value = "${spring.kafka.security.protocol:#{null}}")
	private String kafkaSecurityProtocol;

	@Value(value = "${spring.kafka.sasl.mechanism:#{null}}")
	private String saslMechanism;

	@Value(value = "${spring.kafka.sasl.jaas.config:#{null}}")
	private String saslJaasConfig;

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		configs.put("security.protocol", kafkaSecurityProtocol);
		configs.put("sasl.mechanism", saslMechanism);
		configs.put("sasl.jaas.config", saslJaasConfig);
		return new KafkaAdmin(configs);
	}

	@Bean
	public NewTopic paymentRequestsNewTopic(@Value("${payment.requests.topic}") String paymentRequestsTopic) {
		return new NewTopic(paymentRequestsTopic, 3, (short) 1);
	}
}
