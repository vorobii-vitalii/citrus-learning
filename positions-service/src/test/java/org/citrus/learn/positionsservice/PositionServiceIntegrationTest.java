package org.citrus.learn.positionsservice;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;

import java.util.Map;

import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.channel.ChannelEndpointBuilder;
import org.citrusframework.junit.jupiter.CitrusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mongodb.dsl.MongoDb;
import org.springframework.integration.mongodb.store.MongoDbMessageStore;
import org.springframework.messaging.MessageChannel;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.reactivestreams.client.MongoClients;

import groovy.util.logging.Slf4j;

@Testcontainers
@ExtendWith(CitrusExtension.class)
@Slf4j
public class PositionServiceIntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PositionServiceIntegrationTest.class);

	static Network network = Network.newNetwork();

	@Container
	static MongoDBContainer mongo = new MongoDBContainer().withNetwork(network);

	//	@Container
	//	static GenericContainer<?> paymentService = new LocalJavaContainer<>()
	//			.withNetwork(network)
	//			.dependsOn(mongo)
	//			.withEnv(Map.of(
	//					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
	//							"spring.r2dbc.username", USERNAME,
	//							"spring.r2dbc.password", PASSWORD,
	//							"spring.sql.init.mode", "never",
	//							"spring.r2dbc.url", "r2dbc:postgresql://postgres:5432/%s".formatted(DATABASE_NAME)
	//					)),
	//					"PAYMENT_REQUESTS_TOPIC", PAYMENT_REQUESTS,
	//					"KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"
	//			))
	//			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("payment-service"));

	@Test
	@CitrusTest
	void shouldReturnSenderNotFoundIfBalanceInfoOfSenderNotFound(@CitrusResource TestActionRunner actions) {
//		change
		ReactiveMongoDatabaseFactory mongoDbFactory =
				new SimpleReactiveMongoDatabaseFactory(MongoClients.create(), "test");

		MessageChannel messageChannel = new DirectChannel();

//		IntegrationFlow.from().to(MongoDb.reactiveOutboundChannelAdapter(mongoDbFactory));
//
//		new MongoDbMessageStore(mongoDbFactory, )

		//		actions.$(new ChannelEndpointBuilder()
//				.channel("helloChannel")
//				.messagingTemplate(messagingTemplate())
//				.build())
	}
}