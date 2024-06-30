package org.citrus.learn.positionsservice.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;

@ExtendWith(MockitoExtension.class)
class TestRequestPricesPredicate {

	@Mock
	DataFetchingEnvironment dataFetchingEnvironment;

	@Mock
	DataFetchingFieldSelectionSet dataFetchingFieldSelectionSet;

	RequestPricesPredicate requestPricesPredicate = new RequestPricesPredicate();

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void shouldRequestPricesIfEitherPerformanceOrCurrentPricesAreRequested(boolean isPartOfSelectionSet) {
		when(dataFetchingEnvironment.getSelectionSet()).thenReturn(dataFetchingFieldSelectionSet);
		when(dataFetchingFieldSelectionSet.containsAnyOf(
				DgsConstants.POSITIONDETAILS.Performance,
				DgsConstants.POSITIONDETAILS.Positions + "/" + DgsConstants.POSITION.CurrentPrice
		)).thenReturn(isPartOfSelectionSet);
		assertThat(requestPricesPredicate.shouldRequestPrices(dataFetchingEnvironment)).isEqualTo(isPartOfSelectionSet);
	}
}