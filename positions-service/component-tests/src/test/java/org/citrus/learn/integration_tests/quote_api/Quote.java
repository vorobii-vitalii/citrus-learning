package org.citrus.learn.integration_tests.quote_api;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record Quote(String symbol, BigDecimal unitPrice) {
}
