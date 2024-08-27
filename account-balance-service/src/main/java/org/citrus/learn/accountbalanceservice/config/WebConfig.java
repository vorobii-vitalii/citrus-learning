package org.citrus.learn.accountbalanceservice.config;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.citrus.learn.accountbalanceservice.rest.handler.UserBalanceRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

	@Bean
	RouterFunction<ServerResponse> accountBalanceRouterFunction(UserBalanceRequestHandler handler) {
		return route()
				.GET("/user-balance/{accountId}/{currency}", accept(APPLICATION_JSON), handler::fetchBalanceInCurrency)
				.build();
	}

}
