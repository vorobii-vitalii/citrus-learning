package org.citrus.learn.positionsservice.prices;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface PricesProvider {
	Map<String, BigDecimal> fetchPricesForSymbols(Set<String> symbols);
}
