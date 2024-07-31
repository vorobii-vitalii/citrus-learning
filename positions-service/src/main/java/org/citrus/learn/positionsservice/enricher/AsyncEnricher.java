package org.citrus.learn.positionsservice.enricher;

import java.util.concurrent.CompletableFuture;

public interface AsyncEnricher<T, CTX> {
	CompletableFuture<T> enrich(CompletableFuture<T> existing, CTX context);
	boolean shouldBeApplied(CTX context);
}
