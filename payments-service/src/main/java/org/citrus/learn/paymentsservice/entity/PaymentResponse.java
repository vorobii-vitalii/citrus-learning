package org.citrus.learn.paymentsservice.entity;

import java.util.UUID;

import lombok.Builder;

@Builder
public record PaymentResponse(UUID transactionId, TransactionStatus transactionStatus) {
}
