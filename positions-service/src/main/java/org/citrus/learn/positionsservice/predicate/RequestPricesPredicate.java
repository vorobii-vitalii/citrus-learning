package org.citrus.learn.positionsservice.predicate;

import org.citrus.learn.positionsservice.codegen.DgsConstants;
import org.springframework.stereotype.Component;

import graphql.schema.DataFetchingEnvironment;

@Component
public class RequestPricesPredicate {
	public boolean shouldRequestPrices(DataFetchingEnvironment dataFetchingEnvironment) {
		return dataFetchingEnvironment.getSelectionSet().containsAnyOf(
				DgsConstants.POSITIONDETAILS.Performance,
				DgsConstants.POSITIONDETAILS.Positions + "/" + DgsConstants.POSITION.CurrentPrice
		);
	}
}
