package citrus.learn.actions;

import static org.citrusframework.http.actions.HttpActionBuilder.http;

import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.http.actions.HttpClientResponseActionBuilder;
import org.citrusframework.http.message.HttpMessage;
import org.springframework.http.HttpStatus;

public class AccountBalanceServiceActions {
	private final String accountBalanceServiceURI;

	public AccountBalanceServiceActions(String accountBalanceServiceURI) {
		this.accountBalanceServiceURI = accountBalanceServiceURI;
	}

	public AbstractTestAction requestUserBalance() {
		return http()
				.client(accountBalanceServiceURI)
				.send()
				.get("/user-balance/${accountId}/${currency}")
				.message()
				.header("Content-Type", "application/json")
				.build();
	}

	public HttpClientResponseActionBuilder expectStatusCode(HttpStatus expectedErrorCode) {
		return http().client(accountBalanceServiceURI).receive().response(expectedErrorCode);
	}

	public HttpClientResponseActionBuilder.HttpMessageBuilderSupport expectSuccess() {
		return expectStatusCode(HttpStatus.OK).message(new HttpMessage("${expectedBalance}"));
	}

}
