package org.citrus.learn.paymentsservice.listener;

import org.citrus.learn.paymentsservice.entity.PaymentRequest;
import org.citrus.learn.paymentsservice.entity.PaymentResponse;
import org.citrus.learn.paymentsservice.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestsListener {
	private final PaymentService paymentService;

	@KafkaListener(id = "paymentRequestsListener", topics = "${payment.requests.topic}")
	@SendTo
	public Mono<PaymentResponse> listen(
			@Payload PaymentRequest paymentRequest,
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
			@Header(KafkaHeaders.GROUP_ID) String groupId,
			ConsumerRecordMetadata consumerRecordMetadata
	) {
		log.info("Received new message {} request = {} key = {} groupId = {}", consumerRecordMetadata, paymentRequest, key, groupId);
		return paymentService.performPayment(paymentRequest)
				.map(status -> {
					log.info("Status = {}", status);
					return PaymentResponse.builder()
							.transactionId(paymentRequest.transactionId())
							.transactionStatus(status)
							.build();
				});
	}

}
