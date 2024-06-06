package org.citrus.learn.paymentsservice;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.citrusframework.actions.ExecuteSQLAction.Builder.sql;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.citrus.learn.paymentsservice.actions.AccountBalanceDatabaseActions;
import org.citrus.learn.paymentsservice.actions.PaymentServiceActions;
import org.citrus.learn.paymentsservice.utils.JavaOptionsCreator;
import org.citrus.learn.paymentsservice.utils.LocalJavaContainer;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Testcontainers
@ExtendWith(CitrusExtension.class)
public class PaymentServiceIntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceIntegrationTest.class);
	private static final String DATABASE_NAME = "foo";
	private static final String USERNAME = "foo";
	private static final String PASSWORD = "secret";
	private static final String PAYMENT_REQUESTS = "payment-requests";
	private static final String PAYMENT_RESPONSE = "payment-responses";
	static Network network = Network.newNetwork();

	@Container
	static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>()
			.withNetwork(network)
			.withNetworkAliases("postgres")
			.withDatabaseName(DATABASE_NAME)
			.withUsername(USERNAME)
			.withPassword(PASSWORD)
			.withInitScript("init.sql");

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
			.withNetwork(network)
			.withNetworkAliases("kafka")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("kafka"));

	@Container
	static GenericContainer<?> paymentService = new LocalJavaContainer<>()
			.withNetwork(network)
			.dependsOn(postgreSQLContainer, kafka)
			.withEnv(Map.of(
					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
							"spring.r2dbc.username", USERNAME,
							"spring.r2dbc.password", PASSWORD,
							"spring.sql.init.mode", "never",
							"spring.r2dbc.url", "r2dbc:postgresql://postgres:5432/%s".formatted(DATABASE_NAME)
					)),
					"PAYMENT_REQUESTS_TOPIC", PAYMENT_REQUESTS,
					"KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"
			))
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("payment-service"));

	private static HikariDataSource dataSource;
	private static PaymentServiceActions paymentServiceActions;
	private static AccountBalanceDatabaseActions accountBalanceDatabaseActions;

	@BeforeAll
	static void init() {
		createTopics(PAYMENT_REQUESTS, PAYMENT_RESPONSE);
		initDataSource();
		paymentServiceActions = new PaymentServiceActions(kafka.getBootstrapServers(), PAYMENT_REQUESTS, PAYMENT_RESPONSE);
		accountBalanceDatabaseActions = new AccountBalanceDatabaseActions(dataSource);
	}

	private static void initDataSource() {
		var config = new HikariConfig();
		config.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
		config.setUsername(postgreSQLContainer.getUsername());
		config.setPassword(postgreSQLContainer.getPassword());
		config.setAutoCommit(true);
		dataSource = new HikariDataSource(config);
	}

	private static void createTopics(String... topics) {
		var newTopics = Arrays.stream(topics).map(topic -> new NewTopic(topic, 1, (short) 1)).toList();
		try (var admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
			admin.createTopics(newTopics);
		}
	}

	@Test
	@CitrusTest
	void shouldReturnSenderNotFoundIfBalanceInfoOfSenderNotFound(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("expectedTransactionStatus", "SENDER_NOT_FOUND");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info"));
		actions.$(paymentServiceActions.sendPaymentRequest());
		actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
	}

	@Test
	@CitrusTest
	void shouldReturnReceiverNotFoundIfBalanceInfoOfReceiverNotFound(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("fromAccountBalanceBeforeTransaction", "900");
			builder.setVariable("expectedSenderBalanceAfterTransaction", "900");
			builder.setVariable("expectedTransactionStatus", "RECEIVER_NOT_FOUND");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(accountBalanceDatabaseActions.insertBalanceForSender());
		actions.$(sql().dataSource(dataSource).statement("delete from user_info"));
		actions.$(paymentServiceActions.sendPaymentRequest());
		actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfSender());
	}

	@Test
	@CitrusTest
	void shouldReturnInsufficientFundsIfSenderDoesNotHaveEnoughFundsOnBalance(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("fromAccountBalanceBeforeTransaction", "40");
			builder.setVariable("toAccountBalanceBeforeTransaction", "900");
			builder.setVariable("expectedSenderBalanceAfterTransaction", "40");
			builder.setVariable("expectedReceiverBalanceAfterTransaction", "900");
			builder.setVariable("expectedTransactionStatus", "INSUFFICIENT_FUNDS");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(accountBalanceDatabaseActions.insertBalanceForSender());
		actions.$(accountBalanceDatabaseActions.insertBalanceForReceiver());
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
		);
		actions.$(paymentServiceActions.sendPaymentRequest());
		actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfSender());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfReceiver());
	}

	@Test
	@CitrusTest
	void shouldReturnSenderBlockedIfSenderBlocked(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("fromAccountBalanceBeforeTransaction", "130");
			builder.setVariable("toAccountBalanceBeforeTransaction", "900");
			builder.setVariable("expectedSenderBalanceAfterTransaction", "130");
			builder.setVariable("expectedReceiverBalanceAfterTransaction", "900");
			builder.setVariable("expectedTransactionStatus", "SENDER_BLOCKED");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(accountBalanceDatabaseActions.insertBalanceForSender());
		actions.$(accountBalanceDatabaseActions.insertBalanceForReceiver());
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, true)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		actions.$(paymentServiceActions.sendPaymentRequest());
		actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfSender());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfReceiver());
	}

	@Test
	@CitrusTest
	void shouldReturnReceiverBlockedIfReceiverBlocked(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("fromAccountBalanceBeforeTransaction", "130");
			builder.setVariable("toAccountBalanceBeforeTransaction", "900");
			builder.setVariable("expectedSenderBalanceAfterTransaction", "130");
			builder.setVariable("expectedReceiverBalanceAfterTransaction", "900");
			builder.setVariable("expectedTransactionStatus", "RECEIVER_BLOCKED");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(accountBalanceDatabaseActions.insertBalanceForSender());
		actions.$(accountBalanceDatabaseActions.insertBalanceForReceiver());
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, true)")
		);
		actions.$(paymentServiceActions.sendPaymentRequest());
		actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfSender());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfReceiver());
	}

	@Test
	@CitrusTest
	void shouldExecuteTransactionIfNoneOfParticipantsBlockedAndSenderHasEnoughMoney(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("fromAccountBalanceBeforeTransaction", "130");
			builder.setVariable("toAccountBalanceBeforeTransaction", "900");
			builder.setVariable("expectedTransactionStatus", "SUCCESS");
			builder.setVariable("expectedSenderBalanceAfterTransaction", "30");
			builder.setVariable("expectedReceiverBalanceAfterTransaction", "1000");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(accountBalanceDatabaseActions.insertBalanceForSender());
		actions.$(accountBalanceDatabaseActions.insertBalanceForReceiver());
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		actions.$(paymentServiceActions.sendPaymentRequest());
		actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfSender());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfReceiver());
	}

	@Test
	@CitrusTest
	void processingOfTransactionShouldBeIdempotent(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("transactionId", UUID.randomUUID().toString());
			builder.setVariable("fromAccountId", "1");
			builder.setVariable("toAccountId", "2");
			builder.setVariable("transactionAmount", "100");
			builder.setVariable("fromAccountBalanceBeforeTransaction", "5000");
			builder.setVariable("toAccountBalanceBeforeTransaction", "900");
			builder.setVariable("expectedTransactionStatus", "SUCCESS");
			builder.setVariable("expectedSenderBalanceAfterTransaction", "4900");
			builder.setVariable("expectedReceiverBalanceAfterTransaction", "1000");
		});
		actions.$(accountBalanceDatabaseActions.cleanExistingBalances());
		actions.$(accountBalanceDatabaseActions.insertBalanceForSender());
		actions.$(accountBalanceDatabaseActions.insertBalanceForReceiver());
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		for (int i = 0; i < 5; i++) {
			actions.$(paymentServiceActions.sendPaymentRequest());
			actions.$(paymentServiceActions.expectResponseWithCorrectStatus());
		}
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfSender());
		actions.$(accountBalanceDatabaseActions.verifyBalanceOfReceiver());
	}

}
