package org.citrus.learn.positionsservice.client.finnhub;

import java.math.BigDecimal;

public record Quote(String symbol, BigDecimal unitPrice) {
}
