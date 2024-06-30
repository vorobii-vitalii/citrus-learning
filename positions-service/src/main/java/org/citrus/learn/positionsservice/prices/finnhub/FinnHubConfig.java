package org.citrus.learn.positionsservice.prices.finnhub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Configuration
public class FinnHubConfig {

	@Value("${finn.hub.api.url}")
	private String finnHubApiBaseURL;

	@Bean
	QuotesService quotesService() {
		var retrofit = new Retrofit.Builder()
				.baseUrl(finnHubApiBaseURL)
				.addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create()))
				.build();
		return retrofit.create(QuotesService.class);
	}

}
