package org.citrus.learn.paymentsservice.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.citrus.learn.paymentsservice.entity.PaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

	@Value(value = "${spring.kafka.security.protocol:#{null}}")
	private String kafkaSecurityProtocol;

	@Value(value = "${spring.kafka.sasl.mechanism:#{null}}")
	private String saslMechanism;

	@Value(value = "${spring.kafka.sasl.jaas.config:#{null}}")
	private String saslJaasConfig;

	@Bean
	public ConsumerFactory<String, PaymentRequest> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put("security.protocol", kafkaSecurityProtocol);
		props.put("sasl.mechanism", saslMechanism);
		props.put("sasl.jaas.config", saslJaasConfig);
		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(PaymentRequest.class));
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, PaymentRequest> kafkaListenerContainerFactory(
			KafkaTemplate<String, Object> kafkaTemplate
	) {
		var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, PaymentRequest>();
		concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory());
		concurrentKafkaListenerContainerFactory.setReplyTemplate(kafkaTemplate);
		return concurrentKafkaListenerContainerFactory;
	}
}
