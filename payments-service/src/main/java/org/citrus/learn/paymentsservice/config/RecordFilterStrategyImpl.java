package org.citrus.learn.paymentsservice.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

public class RecordFilterStrategyImpl implements RecordFilterStrategy {
	@Override
	public boolean filter(ConsumerRecord consumerRecord) {
		return false;
	}
}
