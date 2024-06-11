package org.citrus.learn.positionsservice.client.finnhub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import retrofit2.Retrofit;

@Configuration
public class FinnHubConfig {

	@Value("${finn.hub.api.url}")
	private String finnHubApiBaseURL;

	@Bean
	QuotesService quotesService() {
		Retrofit retrofit = new Retrofit.Builder()
				.baseUrl(finnHubApiBaseURL)
				.build();
		return retrofit.create(QuotesService.class);
	}

}
