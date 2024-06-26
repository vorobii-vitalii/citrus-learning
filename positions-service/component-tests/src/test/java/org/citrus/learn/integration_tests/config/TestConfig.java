package org.citrus.learn.integration_tests.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;

@Configuration
@EnableIntegration
public class TestConfig {

	@Qualifier("mongoWritesChannel")
	@Bean
	DirectChannel mongoWritesChannel() {
		DirectChannel channel = new DirectChannel();
		channel.setBeanName("mongoWritesChannel");
		return channel;
	}

}
