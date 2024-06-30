package org.citrus.learn.integration_tests;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import org.citrus.learn.integration_tests.utils.JavaOptionsCreator;
import org.mockserver.client.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public abstract class BaseIntegrationTest {
	public static final int GRAPHQL_SERVICE_PORT = 8080;
	public static final Network NETWORK = Network.newNetwork();
	private static final Logger LOGGER = LoggerFactory.getLogger(PositionServiceIntegrationTest.class);
	public static final MockServerContainer MOCK_SERVER =
			new MockServerContainer(
					DockerImageName.parse("mockserver/mockserver")
							.withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion()))
					.withNetwork(NETWORK)
					.withNetworkAliases("mockserver")
					.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mock-server"));

	public static final MongoDBContainer MONGO = new MongoDBContainer()
			.withNetwork(NETWORK)
			.withNetworkAliases("mongo")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("mongo"));

	public static final GenericContainer<?> POSITIONS_SERVICE = new GenericContainer<>(
			new ImageFromDockerfile()
					.withFileFromPath("/app.jar", Paths.get(MountableFile.forHostPath("../build/libs/app.jar").getResolvedPath()))
					.withFileFromPath("/Dockerfile", Paths.get(MountableFile.forHostPath("../Dockerfile").getResolvedPath())))
			.withNetwork(NETWORK)
			.dependsOn(MONGO, MOCK_SERVER)
			.withStartupTimeout(Duration.ofSeconds(7))
			.withEnv(Map.of(
					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
							"finn.hub.api.url", "http://mockserver:1080/",
							"spring.data.mongodb.uri", "mongodb://mongo:27017/test"
					))
			))
			.withExposedPorts(GRAPHQL_SERVICE_PORT)
			.waitingFor(Wait.forListeningPorts(GRAPHQL_SERVICE_PORT))
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("positions-service"));

	static {
		MOCK_SERVER.start();
		MONGO.start();
		POSITIONS_SERVICE.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			MOCK_SERVER.stop();
			MONGO.stop();
			POSITIONS_SERVICE.stop();
			NETWORK.close();
		}));
	}

}
