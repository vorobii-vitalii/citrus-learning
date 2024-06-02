package org.citrus.learn.accountbalanceservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@PropertySource(value = "classpath:external-rate-service.properties")
public class ExternalRateServiceConfig {

	@Value("${baseUrl}")
	private String baseUrl;

	@Qualifier("externalRateClient")
	@Bean
	WebClient externalRateServiceClient() {
		return WebClient.create(baseUrl);
	}

}
