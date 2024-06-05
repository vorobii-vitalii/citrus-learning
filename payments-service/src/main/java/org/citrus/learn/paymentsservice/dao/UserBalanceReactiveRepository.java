package org.citrus.learn.paymentsservice.dao;

import org.citrus.learn.paymentsservice.entity.UserBalance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserBalanceReactiveRepository extends ReactiveCrudRepository<UserBalance, Long> {
}
