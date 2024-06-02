package citrus.learn;

import static org.citrusframework.actions.ExecuteSQLAction.Builder.sql;
import static org.citrusframework.script.GroovyAction.Builder.groovy;

import java.util.Map;

import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import citrus.learn.actions.AccountBalanceServiceActions;
import citrus.learn.actions.MockCurrencyRateServiceActions;
import citrus.utils.JavaOptionsCreator;
import citrus.utils.LocalJavaContainer;

@Testcontainers
@ExtendWith(CitrusExtension.class)
public class AccountBalanceIntegrationTest {
	private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
			.parse("mockserver/mockserver")
			.withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceIntegrationTest.class);
	private static final String DATABASE_NAME = "foo";
	private static final String USERNAME = "foo";
	private static final String PASSWORD = "secret";
	private static final int ACCOUNT_BALANCE_PORT = 8080;
	private static final String VALID_ACCOUNT_ID = "1";

	static Network network = Network.newNetwork();

	@Container
	static public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)
			.withNetwork(network)
			.withNetworkAliases("mockserver")
			.withLogConsumer(new Slf4jLogConsumer(LOGGER));

	@Container
	static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>()
			.withNetwork(network)
			.withNetworkAliases("postgres")
			.withDatabaseName(DATABASE_NAME)
			.withUsername(USERNAME)
			.withPassword(PASSWORD)
			.withInitScript("init.sql");

	@Container
	static GenericContainer<?> accountBalanceService = new LocalJavaContainer<>()
			.withNetwork(network)
			.withNetworkAliases("account-balance-service")
			.dependsOn(postgreSQLContainer, mockServer)
			.withEnv(Map.of(
					"JAVA_OPTS", JavaOptionsCreator.createOptions(Map.of(
							"spring.r2dbc.username", USERNAME,
							"spring.r2dbc.password", PASSWORD,
							"spring.r2dbc.url", "r2dbc:postgresql://postgres:5432/%s".formatted(DATABASE_NAME)
					)),
					"RATE_SERVICE_BASE_URL", "http://mockserver:1080/"
			))
			.withLogConsumer(new Slf4jLogConsumer(LOGGER))
			.withExposedPorts(ACCOUNT_BALANCE_PORT);

	private HikariDataSource dataSource;
	private MockCurrencyRateServiceActions mockCurrencyRateServiceActions;
	private AccountBalanceServiceActions accountBalanceServiceActions;
	private MockServerClient mockServerClient;

	@BeforeEach
	void init() {
		initDataSource();
		mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
		mockCurrencyRateServiceActions = new MockCurrencyRateServiceActions(mockServerClient);
		accountBalanceServiceActions = new AccountBalanceServiceActions(getAccountBalanceEndpoint());
	}

	@AfterEach
	void removeStubs() {
		mockServerClient.reset();
	}

	@Test
	@CitrusTest
	void shouldCorrectlyCalculateBalanceInSpecificCurrencyIfAccountExistsAndRateServiceAvailable(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("accountId", VALID_ACCOUNT_ID);
			builder.setVariable("currency", "EUR");
			builder.setVariable("balanceInUSD", "900.5");
			builder.setVariable("rate", "1.2");
		});
		actions.$(groovy().script("""
					var expectedBalance = ${balanceInUSD} * ${rate}
					context.setVariable("expectedBalance", expectedBalance)
				"""));
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("insert into user_balance(user_id, user_balance) values(${accountId}, ${balanceInUSD})"));
		actions.$(mockCurrencyRateServiceActions.mockRateSuccess());
		actions.$(accountBalanceServiceActions.requestUserBalance());
		actions.$(accountBalanceServiceActions.expectSuccess());
	}

	@Test
	@CitrusTest
	void shouldReturnServerErrorIfRateServiceUnavailable(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("accountId", "1");
			builder.setVariable("currency", "USD");
			builder.setVariable("balanceInUSD", "500");
		});
		actions.$(sql().dataSource(dataSource)
				.statement("delete from user_balance")
				.statement("insert into user_balance(user_id, user_balance) values(${accountId}, ${balanceInUSD})"));
		actions.$(mockCurrencyRateServiceActions.mockServiceFailure());
		actions.$(accountBalanceServiceActions.requestUserBalance());
		actions.$(accountBalanceServiceActions.expectStatusCode(HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@Test
	@CitrusTest
	void shouldReturnNotFoundIfAccountNotExists(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("accountId", "1");
			builder.setVariable("currency", "USD");
		});
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(accountBalanceServiceActions.requestUserBalance());
		actions.$(accountBalanceServiceActions.expectStatusCode(HttpStatus.NOT_FOUND));
	}

	@Test
	@CitrusTest
	void shouldReturnBadRequestIfAccountIdHasInvalidFormat(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("accountId", "hey");
			builder.setVariable("currency", "USD");
		});
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(accountBalanceServiceActions.requestUserBalance());
		actions.$(accountBalanceServiceActions.expectStatusCode(HttpStatus.BAD_REQUEST));
	}

	@Test
	@CitrusTest
	void shouldReturnBadRequestIfCurrencyHasInvalidFormat(@CitrusResource TestActionRunner actions) {
		actions.$(builder -> {
			builder.setVariable("accountId", "123");
			builder.setVariable("currency", "invalid_currency_iso");
		});
		actions.$(sql().dataSource(dataSource).statement("delete from user_balance"));
		actions.$(accountBalanceServiceActions.requestUserBalance());
		actions.$(accountBalanceServiceActions.expectStatusCode(HttpStatus.BAD_REQUEST));
	}

	private void initDataSource() {
		var config = new HikariConfig();
		config.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
		config.setUsername(postgreSQLContainer.getUsername());
		config.setPassword(postgreSQLContainer.getPassword());
		dataSource = new HikariDataSource(config);
	}

	private @NotNull String getAccountBalanceEndpoint() {
		return String.format("http://localhost:%d", accountBalanceService.getMappedPort(ACCOUNT_BALANCE_PORT));
	}

}
