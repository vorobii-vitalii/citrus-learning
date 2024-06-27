package org.citrus.learn;

import static org.citrus.learn.utils.Constants.PAYMENT_REQUESTS;

import java.nio.file.Paths;
import java.util.Map;

import org.citrus.learn.utils.JavaOptionsCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public abstract class BaseIntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceIntegrationTest.class);
	private static final String DATABASE_NAME = "foo";
	private static final String USERNAME = "foo";
	private static final String PASSWORD = "secret";

	public static final Network NETWORK = Network.newNetwork();

	public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>()
			.withNetwork(NETWORK)
			.withNetworkAliases("postgres")
			.withDatabaseName(DATABASE_NAME)
			.withUsername(USERNAME)
			.withPassword(PASSWORD);

	public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"))
			.withNetwork(NETWORK)
			.withNetworkAliases("kafka")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("kafka"));

	public static final GenericContainer<?> LIQUIBASE = new GenericContainer<>(DockerImageName.parse("liquibase/liquibase"))
			.withCommand("--defaults-file=/liquibase/liquibase.properties update")
			.withCopyFileToContainer(MountableFile.forHostPath("../liquibase"), "/liquibase")
			.withEnv(Map.of(
					"LIQUIBASE_COMMAND_URL", "jdbc:postgresql://postgres:5432/" + DATABASE_NAME,
					"LIQUIBASE_COMMAND_USERNAME", USERNAME,
					"LIQUIBASE_COMMAND_PASSWORD", PASSWORD
			))
			.withNetwork(NETWORK)
			.dependsOn(POSTGRES)
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("liquibase"));

	public static final GenericContainer<?> PAYMENT_SERVICE = new GenericContainer<>(new ImageFromDockerfile()
			.withFileFromPath("/app.jar", Paths.get(MountableFile.forHostPath("../build/libs/app.jar").getResolvedPath()))
			.withFileFromPath("/Dockerfile", Paths.get(MountableFile.forHostPath("../Dockerfile").getResolvedPath())))
			.withNetwork(NETWORK)
			.dependsOn(POSTGRES, KAFKA, LIQUIBASE)
			.withEnv(Map.of(
					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
							"spring.r2dbc.username", USERNAME,
							"spring.r2dbc.password", PASSWORD,
							"spring.sql.init.mode", "never",
							"spring.r2dbc.url", "r2dbc:postgresql://postgres:5432/%s".formatted(DATABASE_NAME)
					)),
					"PAYMENT_REQUESTS_TOPIC", PAYMENT_REQUESTS,
					"KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"
			))
			.withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("payment-service"));

	static {
		POSTGRES.start();
		KAFKA.start();
		LIQUIBASE.start();
		PAYMENT_SERVICE.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			POSTGRES.stop();
			KAFKA.stop();
			PAYMENT_SERVICE.stop();
			LIQUIBASE.stop();
			NETWORK.close();
		}));
	}

}
