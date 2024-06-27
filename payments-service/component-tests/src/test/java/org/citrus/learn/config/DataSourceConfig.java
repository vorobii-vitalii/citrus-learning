package org.citrus.learn.config;

import static org.citrus.learn.BaseIntegrationTest.POSTGRES;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

	@Bean
	DataSource dataSource() {
		var config = new HikariConfig();
		config.setJdbcUrl(POSTGRES.getJdbcUrl());
		config.setUsername(POSTGRES.getUsername());
		config.setPassword(POSTGRES.getPassword());
		config.setAutoCommit(true);
		return new HikariDataSource(config);
	}

}
