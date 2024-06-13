package org.citrus.learn.positionsservice.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.citrus.learn.positionsservice.codegen.types.Position;
import org.citrus.learn.positionsservice.domain.ClientPositionCollectionObject;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientPositionsRepository {
	Mono<Map<String, List<Position>>> findPositionsForClients(Set<String> clientIds);
}
