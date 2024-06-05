package org.citrus.learn.paymentsservice.entity;

public enum TransactionStatus {
	INSUFFICIENT_FUNDS,
	SENDER_NOT_FOUND,
	RECEIVER_NOT_FOUND,
	SENDER_BLOCKED,
	RECEIVER_BLOCKED,
	SUCCESS
}
