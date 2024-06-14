package org.citrus.learn.positionsservice.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.service.PositionsPerformanceEvaluator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AverageProfitPositionsPerformanceEvaluator implements PositionsPerformanceEvaluator {
	private static final MathContext MATH_CONTEXT = new MathContext(8);

	@Override
	public BigDecimal evaluatePositionsPerformance(List<Position> positions) {
		log.info("Calculating performance based on positions = {}", positions);
		if (positions.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return positions
				.stream()
				.map(this::calculatePositionPerformance)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO)
				.divide(BigDecimal.valueOf(positions.size()), MATH_CONTEXT);
	}


	private BigDecimal calculatePositionPerformance(Position v) {
		return v.getCurrentPrice().subtract(v.getPurchasePrice()).multiply(v.getQuantity());
	}

}
