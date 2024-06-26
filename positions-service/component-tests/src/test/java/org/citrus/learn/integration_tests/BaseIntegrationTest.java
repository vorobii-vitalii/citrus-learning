package org.citrus.learn.integration_tests;

import java.util.Map;

import org.citrus.learn.integration_tests.utils.JavaOptionsCreator;
import org.citrus.learn.integration_tests.utils.LocalJavaContainer;
import org.mockserver.client.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class BaseIntegrationTest {
	private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
			.parse("mockserver/mockserver")
			.withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
	private static final Logger LOGGER = LoggerFactory.getLogger(PositionServiceIntegrationTest.class);
	public static final int GRAPHQL_SERVICE_PORT = 8080;

	public static final Network NETWORK = Network.newNetwork();

	public static final MockServerContainer MOCK_SERVER = new MockServerContainer(MOCKSERVER_IMAGE)
			.withNetwork(NETWORK)
			.withReuse(true)
			.withNetworkAliases("mockserver")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mock-server"));

	public static final MongoDBContainer MONGO = new MongoDBContainer()
			.withNetwork(NETWORK)
			.withReuse(true)
			.withNetworkAliases("mongo")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mongo"));

	public static final GenericContainer<?> POSITIONS_SERVICE = new LocalJavaContainer<>()
			.withNetwork(NETWORK)
			.dependsOn(MONGO, MOCK_SERVER)
			.withReuse(true)
			.withEnv(Map.of(
					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
							"finn.hub.api.url", "http://mockserver:1080/",
							"spring.data.mongodb.uri", "mongodb://mongo:27017/test"
					))
			))
			.withExposedPorts(GRAPHQL_SERVICE_PORT)
			.waitingFor(Wait.forListeningPorts(GRAPHQL_SERVICE_PORT))
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("payment-service"));

	static {
		MOCK_SERVER.start();
		MONGO.start();
		POSITIONS_SERVICE.start();
	}

}
