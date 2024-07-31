package org.citrus.learn.integration_tests.actions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.math.BigDecimal;
import java.util.Map;

import org.citrus.learn.integration_tests.quote_api.Quote;
import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.context.TestContext;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import com.google.gson.Gson;

public class MockQuoteServiceActions {
	private final MockServerClient mockServerClient;
	private final Gson gson = new Gson();

	public MockQuoteServiceActions(MockServerClient mockServerClient) {
		this.mockServerClient = mockServerClient;
	}

	public AbstractTestAction mockPrices(Map<String, BigDecimal> priceBySymbol) {
		var symbolsArray = priceBySymbol.keySet().toArray(String[]::new);
		var quotes = priceBySymbol.entrySet()
				.stream()
				.map(v -> Quote.builder().symbol(v.getKey()).unitPrice(v.getValue()).build())
				.toList();
		return new AbstractTestAction() {
			@Override
			public void doExecute(TestContext testContext) {
				mockServerClient
						.when(request().withPath("/quotes/bulk").withQueryStringParameter("symbol", symbolsArray))
						.respond(response()
								.withContentType(MediaType.JSON_UTF_8)
								.withBody(gson.toJson(quotes)));
			}
		};
	}

	public AbstractTestAction verifyNotCalled() {
		return new AbstractTestAction() {
			@Override
			public void doExecute(TestContext testContext) {
				mockServerClient.verify(request().withPath("/quotes/bulk"), VerificationTimes.never());
			}
		};
	}

}
