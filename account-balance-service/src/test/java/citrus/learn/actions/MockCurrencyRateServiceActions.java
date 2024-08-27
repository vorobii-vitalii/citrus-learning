package citrus.learn.actions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.context.TestContext;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.http.HttpStatus;

public class MockCurrencyRateServiceActions {
	private final MockServerClient mockServerClient;

	public MockCurrencyRateServiceActions(MockServerClient mockServerClient) {
		this.mockServerClient = mockServerClient;
	}

	public AbstractTestAction mockRateSuccess() {
		return new AbstractTestAction() {
			@Override
			public void doExecute(TestContext testContext) {
				mockServerClient
						.when(request().withPath("/rates/%s/%s".formatted("USD", testContext.getVariable("currency"))))
						.respond(response()
								.withContentType(MediaType.JSON_UTF_8)
								.withBody("""
										{ "rate": %s }
										""".formatted(testContext.getVariable("rate"))));
			}
		};
	}

	public AbstractTestAction mockServiceFailure() {
		return new AbstractTestAction() {
			@Override
			public void doExecute(TestContext testContext) {
				mockServerClient
						.when(request().withPath("/rates/%s/%s".formatted("USD", testContext.getVariable("currency"))))
						.respond(response().withContentType(MediaType.JSON_UTF_8).withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
			}
		};
	}

}
