package org.citrus.learn.paymentsservice.service;

import org.citrus.learn.paymentsservice.entity.PaymentRequest;
import org.citrus.learn.paymentsservice.entity.TransactionStatus;

import reactor.core.publisher.Mono;

public interface PaymentService {
	Mono<TransactionStatus> performPayment(PaymentRequest paymentRequest);
}
