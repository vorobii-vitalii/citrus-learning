package org.citrus.learn.integration_tests.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MongoEndpointConfig.class, PositionsGraphQlServiceEndpointConfig.class, MockServerConfig.class})
public class TestConfig {

}
