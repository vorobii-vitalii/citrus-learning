package org.citrus.learn.positionsservice.dao;

import java.util.UUID;

import org.citrus.learn.positionsservice.domain.ClientPositionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientPositionsRepository extends MongoRepository<ClientPositionEntity, UUID> {
}
