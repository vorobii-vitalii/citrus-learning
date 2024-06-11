package org.citrus.learn.positionsservice.dataloader;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.citrus.learn.positionsservice.client.PricesProvider;
import org.dataloader.MappedBatchLoader;

import com.netflix.graphql.dgs.DgsDataLoader;

import lombok.RequiredArgsConstructor;

@DgsDataLoader(name = "prices")
@RequiredArgsConstructor
public class PricesDataLoader implements MappedBatchLoader<String, BigDecimal> {
	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private final PricesProvider pricesProvider;

	@Override
	public CompletionStage<Map<String, BigDecimal>> load(Set<String> set) {
		return CompletableFuture.supplyAsync(() -> pricesProvider.fetchPricesForSymbols(set), executorService);
	}

}
