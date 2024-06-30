package org.citrus.learn.positionsservice.domain;

import java.math.BigDecimal;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "positions")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClientPositionCollectionObject {

	@MongoId(FieldType.OBJECT_ID)
	private ObjectId positionId;

	private String clientId;

	private BigDecimal quantity;

	private BigDecimal purchasePrice;

	private String symbol;
}
