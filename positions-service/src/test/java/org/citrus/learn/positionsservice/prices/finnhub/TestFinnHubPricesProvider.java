package org.citrus.learn.positionsservice.prices.finnhub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import retrofit2.Call;
import retrofit2.Response;

@ExtendWith(MockitoExtension.class)
class TestFinnHubPricesProvider {

	@Mock
	QuotesService quotesService;

	@InjectMocks
	FinnHubPricesProvider finnHubPricesProvider;

	@Mock
	Call<List<Quote>> quotesCall;

	@Mock
	Response<List<Quote>> quotesResponse;

	@Test
	void shouldCorrectlyHandleResponseFromFinnHubAPI() throws IOException {
		List<String> symbols = List.of("XBT/USD", "SOL/EUR");

		when(quotesService.fetchQuotes(symbols)).thenReturn(quotesCall);
		when(quotesCall.execute()).thenReturn(quotesResponse);
		when(quotesResponse.body()).thenReturn(List.of(
				Quote.builder()
						.symbol("XBT/USD")
						.unitPrice(BigDecimal.valueOf(35000))
						.build(),
				Quote.builder()
						.symbol("SOL/EUR")
						.unitPrice(BigDecimal.valueOf(24000))
						.build()
		));
		Map<String, BigDecimal> priceBySymbol = finnHubPricesProvider.fetchPricesForSymbols(new LinkedHashSet<>(symbols));
		assertThat(priceBySymbol)
				.containsExactlyInAnyOrderEntriesOf(Map.of(
						"XBT/USD", BigDecimal.valueOf(35000),
						"SOL/EUR", BigDecimal.valueOf(24000)
				));
	}
}
