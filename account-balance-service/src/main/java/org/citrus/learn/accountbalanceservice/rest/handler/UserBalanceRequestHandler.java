package org.citrus.learn.accountbalanceservice.rest.handler;

import java.util.regex.Pattern;

import org.citrus.learn.accountbalanceservice.service.AccountBalanceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserBalanceRequestHandler {
	private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$");
	private static final Pattern CURRENCY_ISO_CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");

	private final AccountBalanceService accountBalanceService;

	public Mono<ServerResponse> fetchBalanceInCurrency(ServerRequest request) {
		String accountId = request.pathVariable("accountId");
		if (notMatches(accountId, INT_PATTERN)) {
			return ServerResponse.badRequest().contentType(MediaType.TEXT_PLAIN).bodyValue("Account id has invalid format");
		}
		String currency = request.pathVariable("currency");
		if (notMatches(currency, CURRENCY_ISO_CODE_PATTERN)) {
			return ServerResponse.badRequest().contentType(MediaType.TEXT_PLAIN).bodyValue("Currency code has invalid format");
		}
		return accountBalanceService.findBalanceInSpecificCurrency(Long.parseLong(accountId), currency)
				.flatMap(result -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result))
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	private boolean notMatches(String str, Pattern pattern) {
		return !pattern.matcher(str).matches();
	}

}
