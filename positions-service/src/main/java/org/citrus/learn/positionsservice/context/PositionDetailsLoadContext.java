package org.citrus.learn.positionsservice.context;

import org.citrus.learn.positionsservice.codegen.types.ClientId;

import lombok.Builder;

@Builder
public record PositionDetailsLoadContext(ClientId clientId, boolean fetchPositionsCurrentPrices) {
}
