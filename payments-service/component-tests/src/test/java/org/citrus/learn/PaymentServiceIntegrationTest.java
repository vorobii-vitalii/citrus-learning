package org.citrus.learn;

import static org.citrusframework.actions.ExecuteSQLAction.Builder.sql;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;

import java.util.UUID;

import javax.sql.DataSource;

import org.citrus.learn.utils.Constants;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusEndpoint;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.kafka.message.KafkaMessageHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@CitrusSpringSupport
@ContextConfiguration(classes = CitrusSpringConfig.class)
public class PaymentServiceIntegrationTest {
	private static final String REPLY_TO = "kafka_replyTopic";
	private static final int TIMEOUT_MS = 10000;

	@CitrusEndpoint(name = "paymentRequestsTopicEndpoint")
	Endpoint paymentRequestsTopicEndpoint;

	@CitrusEndpoint(name = "paymentResponsesTopicEndpoint")
	Endpoint paymentResponsesTopicEndpoint;

	@Autowired
	DataSource dataSource;

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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info"));
		actions.$(send(paymentRequestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
		actions.$(receive(paymentResponsesTopicEndpoint)
				.timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})"));
		actions.$(sql().dataSource(dataSource).statement("delete from user_info"));
		actions.$(send(paymentRequestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
		actions.$(receive(paymentResponsesTopicEndpoint)
				.timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}"));
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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})"));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
		);
		actions.$(send(paymentRequestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
		actions.$(receive(paymentResponsesTopicEndpoint)
				.timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}"));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${toAccountId}")
				.validate("balance", "${expectedReceiverBalanceAfterTransaction}"));
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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})"));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, true)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		actions.$(send(paymentRequestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
		actions.$(receive(paymentResponsesTopicEndpoint)
				.timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}"));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${toAccountId}")
				.validate("balance", "${expectedReceiverBalanceAfterTransaction}"));
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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})"));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) values (${toAccountId}, true)")
		);
		actions.$(send(paymentRequestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
		actions.$(receive(paymentResponsesTopicEndpoint)
				.timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}"));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${toAccountId}")
				.validate("balance", "${expectedReceiverBalanceAfterTransaction}"));
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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})"));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		actions.$(send(paymentRequestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
		actions.$(receive(paymentResponsesTopicEndpoint)
				.timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}"));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${toAccountId}")
				.validate("balance", "${expectedReceiverBalanceAfterTransaction}"));
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
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})"));
		actions.$(sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})"));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from transactions")
				.statement("delete from user_info")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${fromAccountId}, false)")
				.statement("insert into user_info(user_id, is_blocked) "
						+ "values (${toAccountId}, false)")
		);
		for (int i = 0; i < 5; i++) {
			actions.$(send(paymentRequestsTopicEndpoint)
					.message(new KafkaMessage("""
							{
								"transactionId": "${transactionId}",
								"fromAccountId": ${fromAccountId},
								"toAccountId": ${toAccountId},
								"amount": ${transactionAmount}
							}
							""")
							.setHeader(REPLY_TO, Constants.PAYMENT_RESPONSES_TOPIC)));
			actions.$(receive(paymentResponsesTopicEndpoint)
					.timeout(TIMEOUT_MS)
					.message(new KafkaMessage("""
							{
								"transactionId": "${transactionId}",
								"transactionStatus": "${expectedTransactionStatus}"
							}
							"""))
					.header(KafkaMessageHeaders.TOPIC, Constants.PAYMENT_RESPONSES_TOPIC));
		}
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}"));
		actions.$(sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${toAccountId}")
				.validate("balance", "${expectedReceiverBalanceAfterTransaction}"));
	}

}
