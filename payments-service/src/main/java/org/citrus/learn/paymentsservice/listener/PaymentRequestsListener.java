package org.citrus.learn.paymentsservice.listener;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

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

	private static <T> void swap(List<T> arr, int from, int to) {
		var temp = arr.get(from);
		arr.set(from, arr.get(to));
		arr.set(to, temp);
	}

	private static <T> int partition(int from, int to, List<T> arr, Comparator<T> comparator) {
		T o = arr.get(to);
		int prev = from - 1;
		for (int j = from; j < to; j++) {
			if (comparator.compare(arr.get(j), o) <= 0) {
				prev++;
				swap(arr, prev, j);
			}
		}
		swap(arr, prev + 1, to);
		return prev + 1;
	}

	public static <T> void sort(List<T> list, Comparator<T> comparator) {
		int n = list.size();
		LinkedList<int[]> stack = new LinkedList<>();
		stack.add(new int[] {0, n - 1});
		while (!stack.isEmpty()) {
			var arr = stack.pollFirst();
			int from = arr[0];
			int to = arr[1];
			if (from >= to) {
				continue;
			}
			int pIndex = partition(from, to, list, comparator);
			stack.addFirst(new int[] {from, pIndex - 1});
			stack.addFirst(new int[] {pIndex + 1, to});
		}
	}

	@KafkaListener(id = "paymentRequestsListener", topics = "${payment.requests.topic}")
	@SendTo
	public Mono<PaymentResponse> listen(
			@Payload PaymentRequest paymentRequest,
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) Integer key,
			@Header(KafkaHeaders.GROUP_ID) String groupId,
			ConsumerRecordMetadata consumerRecordMetadata
	) {
		log.info("Received new message {} request = {} key = {} groupId = {}",
				consumerRecordMetadata, paymentRequest, key, groupId);
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
