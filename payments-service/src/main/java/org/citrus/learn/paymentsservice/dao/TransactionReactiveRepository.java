package org.citrus.learn.paymentsservice.dao;

import java.util.UUID;

import org.citrus.learn.paymentsservice.entity.Transaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TransactionReactiveRepository extends ReactiveCrudRepository<Transaction, UUID> {
}
