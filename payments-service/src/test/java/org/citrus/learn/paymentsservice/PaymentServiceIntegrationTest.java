package org.citrus.learn.paymentsservice;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.citrusframework.actions.ExecuteSQLAction.Builder.sql;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.citrus.learn.paymentsservice.utils.JavaOptionsCreator;
import org.citrus.learn.paymentsservice.utils.LocalJavaContainer;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusExtension;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.endpoint.KafkaEndpointBuilder;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.kafka.message.KafkaMessageHeaders;
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
	public static final String REPLY_TO = "kafka_replyTopic";
	public static final int TIMEOUT_MS = 10000;
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
	private static KafkaEndpoint requestsTopicEndpoint;
	private static KafkaEndpoint responsesTopicEndpoint;

	@BeforeAll
	static void init() {
		createTopics(PAYMENT_REQUESTS, PAYMENT_RESPONSE);
		initDataSource();
		requestsTopicEndpoint = new KafkaEndpointBuilder().server(kafka.getBootstrapServers()).topic(PAYMENT_REQUESTS).build();
		responsesTopicEndpoint = new KafkaEndpointBuilder().server(kafka.getBootstrapServers()).topic(PAYMENT_RESPONSE).build();
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
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("delete from transactions")
				.statement("delete from user_info"));
		actions.$(send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, PAYMENT_RESPONSE)));

		actions.$(receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "SENDER_NOT_FOUND"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, PAYMENT_RESPONSE));
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
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})")
		);
		actions.$(send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, PAYMENT_RESPONSE)));

		actions.$(receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "RECEIVER_NOT_FOUND"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, PAYMENT_RESPONSE));
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
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})")
		);
		actions.$(send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, PAYMENT_RESPONSE)));

		actions.$(receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "INSUFFICIENT_FUNDS"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, PAYMENT_RESPONSE));
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
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, true)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		actions.$(send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, PAYMENT_RESPONSE)));

		actions.$(receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "SENDER_BLOCKED"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, PAYMENT_RESPONSE));
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
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, true)")
		);
		actions.$(send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, PAYMENT_RESPONSE)));

		actions.$(receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "RECEIVER_BLOCKED"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, PAYMENT_RESPONSE));
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
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})")
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		actions.$(send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, PAYMENT_RESPONSE)));

		actions.$(receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "SUCCESS"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, PAYMENT_RESPONSE));
	}

}
