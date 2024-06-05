package org.citrus.learn.paymentsservice.service.impl;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.citrus.learn.paymentsservice.dao.TransactionReactiveRepository;
import org.citrus.learn.paymentsservice.dao.UserBalanceReactiveRepository;
import org.citrus.learn.paymentsservice.dao.UserInfoReactiveRepository;
import org.citrus.learn.paymentsservice.entity.PaymentRequest;
import org.citrus.learn.paymentsservice.entity.Transaction;
import org.citrus.learn.paymentsservice.entity.TransactionStatus;
import org.citrus.learn.paymentsservice.entity.UserBalance;
import org.citrus.learn.paymentsservice.entity.UserInfo;
import org.citrus.learn.paymentsservice.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
	private final TransactionReactiveRepository transactionRepository;
	private final UserBalanceReactiveRepository userBalanceRepository;
	private final UserInfoReactiveRepository userInfoRepository;

	@Transactional
	@Override
	public Mono<TransactionStatus> performPayment(PaymentRequest paymentRequest) {
		log.info("Processing payment request = {}", paymentRequest);
		var transactionId = paymentRequest.transactionId();
		return transactionRepository.findById(transactionId)
				.doOnNext(transaction -> log.info("Found existing transaction {}", transaction))
				.map(Transaction::getStatus)
				.switchIfEmpty(executeTransaction(paymentRequest).flatMap(status -> saveTransaction(status, transactionId))
						.map(Transaction::getStatus));
	}

	private Mono<Transaction> saveTransaction(TransactionStatus status, UUID transactionId) {
		return transactionRepository.save(new Transaction(transactionId, status));
	}

	private Mono<TransactionStatus> executeTransaction(PaymentRequest paymentRequest) {
		log.info("Executing transaction {} since it doesnt exist yet", paymentRequest);
		var fromAccountId = paymentRequest.fromAccountId();
		var toAccountId = paymentRequest.toAccountId();
		var accountIds = List.of(fromAccountId, toAccountId);
		return userBalanceRepository.findAllById(accountIds)
				.buffer()
				.switchIfEmpty(Flux.just(List.of()))
				.map(v -> extract(v, fromAccountId, toAccountId, UserBalance::getUserId))
				.flatMap(userBalanceResult -> userBalanceResult.process((senderBalance, receiverBalance) -> {
					if (!senderBalance.hasEnoughFunds(paymentRequest.amount())) {
						log.info("Sender {} has insufficient funds {}", fromAccountId, senderBalance.getBalance());
						return Mono.just(TransactionStatus.INSUFFICIENT_FUNDS);
					}
					return userInfoRepository.findAllById(accountIds)
							.buffer()
							.switchIfEmpty(Flux.just(List.of()))
							.map(userInfos -> extract(userInfos, fromAccountId, toAccountId, UserInfo::getId))
							.flatMap(userInfoResult -> userInfoResult.process((senderUserInfo, receiverUserInfo) -> {
								if (senderUserInfo.isBlocked()) {
									log.info("Sender {} is blocked", fromAccountId);
									return Mono.just(TransactionStatus.SENDER_BLOCKED);
								}
								if (receiverUserInfo.isBlocked()) {
									log.info("Receiver {} is blocked", toAccountId);
									return Mono.just(TransactionStatus.RECEIVER_BLOCKED);
								}
								log.info("Transaction is successful!");
								senderBalance.withdrawn(paymentRequest.amount());
								receiverBalance.deposit(paymentRequest.amount());
								return userBalanceRepository.saveAll(List.of(senderBalance, receiverBalance))
										.last()
										.flatMap(v -> Mono.just(TransactionStatus.SUCCESS));
							}))
							.next();
				}))
				.next();
	}

	private <T> TransactionParticipantsResult<T> extract(List<T> list, long senderId, long receiverId, Function<T, Long> idExtractor) {
		var resByUserId = list.stream().collect(Collectors.toMap(idExtractor, v -> v));
		log.info("Extraction result {}", resByUserId);
		return new TransactionParticipantsResult<>(ofNullable(resByUserId.get(senderId)), ofNullable(resByUserId.get(receiverId)));
	}

	private record TransactionParticipantsResult<T>(Optional<T> resultForSender, Optional<T> resultForReceiver) {

		public Mono<TransactionStatus> process(BiFunction<T, T, Mono<TransactionStatus>> function) {
			if (resultForSender.isEmpty()) {
				log.info("Result for sender not found...");
				return Mono.just(TransactionStatus.SENDER_NOT_FOUND);
			}
			if (resultForReceiver.isEmpty()) {
				log.info("Result for receiver not found...");
				return Mono.just(TransactionStatus.RECEIVER_NOT_FOUND);
			}
			return function.apply(resultForSender.get(), resultForReceiver.get());
		}

	}

}
