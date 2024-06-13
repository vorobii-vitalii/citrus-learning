package org.citrus.learn.positionsservice.domain;

import java.math.BigDecimal;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class ClientPositionEntity {

	@Id
	private ObjectId id;

	private String clientId;

	private BigDecimal quantity;

	private String symbol;

}
