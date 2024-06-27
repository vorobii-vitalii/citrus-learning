package org.citrus.learn;

import static org.citrusframework.actions.ExecuteSQLAction.Builder.sql;

import java.util.UUID;

import org.citrus.learn.actions.AccountBalanceDatabaseActions;
import org.citrus.learn.actions.PaymentServiceActions;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariDataSource;

@Testcontainers
@CitrusSpringSupport
@ContextConfiguration(classes = CitrusSpringConfig.class)
public class PaymentServiceIntegrationTest {

	@Autowired
	private HikariDataSource dataSource;

	@Autowired
	private PaymentServiceActions paymentServiceActions;

	@Autowired
	private AccountBalanceDatabaseActions accountBalanceDatabaseActions;

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
