package org.citrus.learn.accountbalanceservice.dao;

import org.citrus.learn.accountbalanceservice.entity.UserBalance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

public interface ReactiveUserBalanceRepository extends ReactiveCrudRepository<UserBalance, Long> {
	Mono<UserBalance> findByUserId(long userId);
}
