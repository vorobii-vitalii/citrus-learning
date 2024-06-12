package org.citrus.learn.positionsservice.dao;

import java.util.UUID;

import org.citrus.learn.positionsservice.domain.ClientPositionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ClientPositionsRepository extends ReactiveCrudRepository<ClientPositionEntity, UUID> {
}
