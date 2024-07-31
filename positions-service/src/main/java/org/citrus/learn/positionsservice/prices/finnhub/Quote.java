package org.citrus.learn.positionsservice.prices.finnhub;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record Quote(String symbol, BigDecimal unitPrice) {
}
