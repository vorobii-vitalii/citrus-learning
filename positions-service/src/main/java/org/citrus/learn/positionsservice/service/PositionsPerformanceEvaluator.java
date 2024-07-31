package org.citrus.learn.positionsservice.service;

import java.math.BigDecimal;
import java.util.List;

import org.citrus.learn.positionsservice.codegen.types.Position;

public interface PositionsPerformanceEvaluator {
	BigDecimal evaluatePositionsPerformance(List<Position> positions);
}
