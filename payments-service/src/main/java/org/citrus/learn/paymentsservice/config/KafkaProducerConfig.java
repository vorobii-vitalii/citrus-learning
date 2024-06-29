package org.citrus.learn.paymentsservice.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {
	private static final int LINGER_MS = 50;

	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

	@Value(value = "${spring.kafka.security.protocol:}")
	private String kafkaSecurityProtocol;

	@Value(value = "${spring.kafka.sasl.mechanism:}")
	private String saslMechanism;

	@Value(value = "${spring.kafka.sasl.jaas.config:}")
	private String saslJaasConfig;

	@Bean
	public Map<String, Object> producerConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		props.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
		ifSet(v -> props.put("security.protocol", v), kafkaSecurityProtocol);
		ifSet(v -> props.put("sasl.mechanism", v), saslMechanism);
		ifSet(v -> props.put("sasl.jaas.config", v), saslJaasConfig);
		return props;
	}

	private void ifSet(Consumer<String> consumer, String v) {
		if (v != null && !v.isEmpty()) {
			consumer.accept(v);
		}
	}

	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		return new DefaultKafkaProducerFactory<>(producerConfigs(), new StringSerializer(), new JsonSerializer<>());
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

}
