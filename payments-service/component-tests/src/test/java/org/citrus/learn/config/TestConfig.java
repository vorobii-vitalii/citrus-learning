package org.citrus.learn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DataSourceConfig.class, KafkaConfig.class})
public class TestConfig {
}
