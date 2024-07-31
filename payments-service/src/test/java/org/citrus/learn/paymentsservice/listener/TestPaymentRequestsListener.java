package org.citrus.learn.paymentsservice.listener;

import static org.mockito.Mockito.when;

import java.util.UUID;

import org.citrus.learn.paymentsservice.entity.PaymentRequest;
import org.citrus.learn.paymentsservice.entity.PaymentResponse;
import org.citrus.learn.paymentsservice.entity.TransactionStatus;
import org.citrus.learn.paymentsservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TestPaymentRequestsListener {
	private static final String KEY = "key";
	private static final String GROUP_ID = "groupId";
	private static final UUID TRANSACTION_ID = UUID.randomUUID();
	private static final TransactionStatus TRANSACTION_STATUS = TransactionStatus.SUCCESS;

	@Mock
	PaymentService paymentService;

	@InjectMocks
	PaymentRequestsListener paymentRequestsListener;

	@Mock
	ConsumerRecordMetadata consumerRecordMetadata;

	@Test
	void shouldDelegateProcessingToPaymentService() {
		var paymentRequest = PaymentRequest.builder().transactionId(TRANSACTION_ID).build();
		when(paymentService.performPayment(paymentRequest)).thenReturn(Mono.just(TRANSACTION_STATUS));
		StepVerifier.create(paymentRequestsListener.listen(paymentRequest, KEY, GROUP_ID, consumerRecordMetadata))
				.expectNext(PaymentResponse.builder().transactionStatus(TRANSACTION_STATUS).transactionId(TRANSACTION_ID).build())
				.expectComplete()
				.log()
				.verify();
	}
}
