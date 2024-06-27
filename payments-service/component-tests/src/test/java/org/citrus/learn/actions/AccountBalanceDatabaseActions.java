package org.citrus.learn.actions;

import static org.citrusframework.actions.ExecuteSQLAction.Builder.sql;

import javax.sql.DataSource;

import org.citrusframework.actions.ExecuteSQLAction;
import org.citrusframework.actions.ExecuteSQLQueryAction;

public class AccountBalanceDatabaseActions {

	private final DataSource dataSource;

	public AccountBalanceDatabaseActions(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public ExecuteSQLAction.Builder cleanExistingBalances() {
		return sql().dataSource(dataSource).statement("delete from user_balance");
	}

	public ExecuteSQLAction.Builder insertBalanceForSender() {
		return sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${fromAccountId}, ${fromAccountBalanceBeforeTransaction})");
	}

	public ExecuteSQLAction.Builder insertBalanceForReceiver() {
		return sql()
				.dataSource(dataSource)
				.statement("insert into user_balance(user_id, user_balance) "
						+ "values (${toAccountId}, ${toAccountBalanceBeforeTransaction})");
	}

	public ExecuteSQLQueryAction.Builder verifyBalanceOfSender() {
		return sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${fromAccountId}")
				.validate("balance", "${expectedSenderBalanceAfterTransaction}");
	}

	public ExecuteSQLQueryAction.Builder verifyBalanceOfReceiver() {
		return sql()
				.dataSource(dataSource)
				.query()
				.statement("select u.user_balance as balance from user_balance u where user_id = ${toAccountId}")
				.validate("balance", "${expectedReceiverBalanceAfterTransaction}");
	}


}
