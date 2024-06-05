package org.citrus.learn.paymentsservice.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(UUID transactionId, long fromAccountId, long toAccountId, BigDecimal amount) {
}
