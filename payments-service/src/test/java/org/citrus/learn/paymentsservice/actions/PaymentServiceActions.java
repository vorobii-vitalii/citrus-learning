package org.citrus.learn.paymentsservice.actions;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;

import org.citrusframework.actions.ReceiveMessageAction;
import org.citrusframework.actions.SendMessageAction;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.endpoint.KafkaEndpointBuilder;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.kafka.message.KafkaMessageHeaders;

public class PaymentServiceActions {
	private static final String REPLY_TO = "kafka_replyTopic";
	private static final int TIMEOUT_MS = 10000;

	private final KafkaEndpoint requestsTopicEndpoint;
	private final KafkaEndpoint responsesTopicEndpoint;

	public PaymentServiceActions(String kafkaBootstrapServers, String paymentRequestsTopic, String paymentResponseTopic) {
		this.requestsTopicEndpoint = new KafkaEndpointBuilder().server(kafkaBootstrapServers).topic(paymentRequestsTopic).build();
		this.responsesTopicEndpoint = new KafkaEndpointBuilder().server(kafkaBootstrapServers).topic(paymentResponseTopic).build();
	}

	public SendMessageAction.SendMessageActionBuilderSupport sendPaymentRequest() {
		return send(requestsTopicEndpoint)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"fromAccountId": ${fromAccountId},
							"toAccountId": ${toAccountId},
							"amount": ${transactionAmount}
						}
						""")
						.setHeader(REPLY_TO, responsesTopicEndpoint.getEndpointConfiguration().getTopic()));
	}

	public ReceiveMessageAction.ReceiveMessageActionBuilderSupport expectResponseWithCorrectStatus() {
		return receive(responsesTopicEndpoint).timeout(TIMEOUT_MS)
				.message(new KafkaMessage("""
						{
							"transactionId": "${transactionId}",
							"transactionStatus": "${expectedTransactionStatus}"
						}
						"""))
				.header(KafkaMessageHeaders.TOPIC, responsesTopicEndpoint.getEndpointConfiguration().getTopic());
	}

}
