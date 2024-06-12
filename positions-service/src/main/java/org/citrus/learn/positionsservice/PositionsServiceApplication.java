package org.citrus.learn.positionsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class PositionsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PositionsServiceApplication.class, args);
	}

}
