package org.citrus.learn.config;

import static org.citrus.learn.BaseIntegrationTest.KAFKA;
import static org.citrus.learn.utils.Constants.PAYMENT_REQUESTS;
import static org.citrus.learn.utils.Constants.PAYMENT_RESPONSE;

import javax.sql.DataSource;

import org.citrus.learn.actions.AccountBalanceDatabaseActions;
import org.citrus.learn.actions.PaymentServiceActions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DataSourceConfig.class, KafkaConfig.class})
public class TestConfig {

	@Bean
	AccountBalanceDatabaseActions accountBalanceDatabaseActions(DataSource dataSource) {
		return new AccountBalanceDatabaseActions(dataSource);
	}

	@Bean
	PaymentServiceActions paymentServiceActions() {
		return new PaymentServiceActions(KAFKA.getBootstrapServers(), PAYMENT_REQUESTS, PAYMENT_RESPONSE);
	}

}
