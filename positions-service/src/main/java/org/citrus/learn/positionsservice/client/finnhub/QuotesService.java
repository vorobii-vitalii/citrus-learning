package org.citrus.learn.positionsservice.client.finnhub;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface QuotesService {
	@GET("/quotes/bulk")
	Call<List<Quote>> fetchQuotes(@Query("symbol") List<String> symbols);
}
