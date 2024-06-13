package org.citrus.learn.positionsservice.datafetchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.citrus.learn.positionsservice.codegen.types.ClientId;
import org.citrus.learn.positionsservice.context.PositionDetailsLoadContext;
import org.citrus.learn.positionsservice.predicate.RequestPricesPredicate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import graphql.schema.DataFetchingEnvironment;

@ExtendWith(MockitoExtension.class)
class TestPositionDetailsDataFetcher {
	private static final ClientId CLIENT_ID = ClientId.newBuilder().id("123").build();

	@Mock
	RequestPricesPredicate requestPricesPredicate;

	@InjectMocks
	PositionDetailsDataFetcher positionDetailsDataFetcher;

	@Mock
	DataFetchingEnvironment dataFetchingEnvironment;

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void shouldCorrectlyCreateContextForChildFetchers(boolean shouldRequestPrices) {
		when(requestPricesPredicate.shouldRequestPrices(dataFetchingEnvironment)).thenReturn(shouldRequestPrices);
		var result = positionDetailsDataFetcher.getPositionDetails(dataFetchingEnvironment, CLIENT_ID);
		assertThat(result.getLocalContext())
				.isEqualTo(
						PositionDetailsLoadContext.builder()
								.fetchPositionsCurrentPrices(shouldRequestPrices)
								.clientId(CLIENT_ID)
								.build());
	}
}
