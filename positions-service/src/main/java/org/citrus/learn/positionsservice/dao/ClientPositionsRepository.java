package org.citrus.learn.positionsservice.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.citrus.learn.positionsservice.codegen.types.Position;

import reactor.core.publisher.Mono;

public interface ClientPositionsRepository {
	Mono<Map<String, List<Position>>> findPositionsForClients(Set<String> clientIds);
}
