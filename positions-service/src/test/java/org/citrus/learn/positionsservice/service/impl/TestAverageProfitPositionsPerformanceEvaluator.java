package org.citrus.learn.positionsservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.junit.jupiter.api.Test;

class TestAverageProfitPositionsPerformanceEvaluator {

	AverageProfitPositionsPerformanceEvaluator performanceEvaluator = new AverageProfitPositionsPerformanceEvaluator();

	@Test
	void shouldReturnZeroIfListOfPositionsIsEmpty() {
		assertThat(performanceEvaluator.evaluatePositionsPerformance(List.of())).isZero();
	}

	@Test
	void shouldCorrectlyEvaluatePositionsPerformance() {
		BigDecimal actualPerformance = performanceEvaluator.evaluatePositionsPerformance(List.of(
				Position.newBuilder()
						.currentPrice(BigDecimal.valueOf(5L))
						.purchasePrice(BigDecimal.valueOf(3L))
						.quantity(BigDecimal.ONE)
						.build(),
				Position.newBuilder()
						.currentPrice(BigDecimal.valueOf(14L))
						.purchasePrice(BigDecimal.valueOf(10L))
						.quantity(BigDecimal.ONE)
						.build()
		));
		assertThat(actualPerformance).isEqualTo("3");
	}
}
