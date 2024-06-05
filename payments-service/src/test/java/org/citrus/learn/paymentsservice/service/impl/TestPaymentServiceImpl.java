package org.citrus.learn.paymentsservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.citrus.learn.paymentsservice.dao.TransactionReactiveRepository;
import org.citrus.learn.paymentsservice.dao.UserBalanceReactiveRepository;
import org.citrus.learn.paymentsservice.dao.UserInfoReactiveRepository;
import org.citrus.learn.paymentsservice.entity.PaymentRequest;
import org.citrus.learn.paymentsservice.entity.Transaction;
import org.citrus.learn.paymentsservice.entity.TransactionStatus;
import org.citrus.learn.paymentsservice.entity.UserBalance;
import org.citrus.learn.paymentsservice.entity.UserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TestPaymentServiceImpl {
	private static final UUID TRANSACTION_ID = UUID.randomUUID();
	private static final long FROM_ACCOUNT_ID = 1L;
	private static final long TO_ACCOUNT_ID = 2L;
	private static final BigDecimal AMOUNT = BigDecimal.valueOf(15);
	public static final BigDecimal TRANSACTION_AMOUNT = AMOUNT;
	private static final PaymentRequest PAYMENT_REQUEST = PaymentRequest.builder()
			.transactionId(TRANSACTION_ID)
			.fromAccountId(FROM_ACCOUNT_ID)
			.toAccountId(TO_ACCOUNT_ID)
			.amount(AMOUNT)
			.build();
	@Mock
	TransactionReactiveRepository transactionRepository;

	@Mock
	UserBalanceReactiveRepository userBalanceRepository;

	@Mock
	UserInfoReactiveRepository userInfoRepository;

	@InjectMocks
	PaymentServiceImpl paymentService;

	@ParameterizedTest
	@EnumSource(TransactionStatus.class)
	void shouldReturnStatusOfExistingTransactionIfItsPresent(TransactionStatus transactionStatus) {
		when(transactionRepository.findById(TRANSACTION_ID))
				.thenReturn(Mono.just(Transaction.builder()
						.id(TRANSACTION_ID)
						.status(transactionStatus)
						.build()));
		StepVerifier.create(paymentService.performPayment(PAYMENT_REQUEST))
				.expectNext(transactionStatus)
				.expectComplete()
				.log()
				.verify();
		verifyNoInteractions(userBalanceRepository, userBalanceRepository);
	}

	@Test
	void shouldReturnStatusSenderNotFoundIfBalanceOfSenderNotFound() {
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Mono.empty());
		when(userBalanceRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserBalance.builder()
						.userId(TO_ACCOUNT_ID)
						.balance(BigDecimal.TEN)
						.build()
		));
		when(transactionRepository.save(any())).thenAnswer(v -> Mono.just(v.getArgument(0)));
		performPaymentAndVerifyStatus(TransactionStatus.SENDER_NOT_FOUND);
		verifyTransactionWasSavedWithStatus(TransactionStatus.SENDER_NOT_FOUND);
	}

	@Test
	void shouldReturnStatusSenderNotFoundIfBalanceOfReceiverNotFound() {
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Mono.empty());
		when(userBalanceRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserBalance.builder()
						.userId(FROM_ACCOUNT_ID)
						.balance(BigDecimal.TEN)
						.build()
		));
		when(transactionRepository.save(any())).thenAnswer(v -> Mono.just(v.getArgument(0)));
		performPaymentAndVerifyStatus(TransactionStatus.RECEIVER_NOT_FOUND);
		verifyTransactionWasSavedWithStatus(TransactionStatus.RECEIVER_NOT_FOUND);
	}

	@Test
	void shouldReturnStatusInsufficientFundsIfSenderDoesNotHaveEnoughMoneyOnBalance() {
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Mono.empty());
		when(userBalanceRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserBalance.builder()
						.userId(FROM_ACCOUNT_ID)
						.balance(TRANSACTION_AMOUNT.subtract(BigDecimal.TEN))
						.build(),
				UserBalance.builder()
						.userId(TO_ACCOUNT_ID)
						.balance(BigDecimal.TEN)
						.build()
		));
		when(transactionRepository.save(any())).thenAnswer(v -> Mono.just(v.getArgument(0)));
		performPaymentAndVerifyStatus(TransactionStatus.INSUFFICIENT_FUNDS);
		verifyTransactionWasSavedWithStatus(TransactionStatus.INSUFFICIENT_FUNDS);
	}

	@Test
	void shouldReturnStatusSenderBlockedIfSenderBlocked() {
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Mono.empty());
		when(userBalanceRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserBalance.builder()
						.userId(FROM_ACCOUNT_ID)
						.balance(TRANSACTION_AMOUNT.add(BigDecimal.TEN))
						.build(),
				UserBalance.builder()
						.userId(TO_ACCOUNT_ID)
						.balance(BigDecimal.TEN)
						.build()
		));
		when(userInfoRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserInfo.builder()
						.id(FROM_ACCOUNT_ID)
						.isBlocked(true)
						.build(),
				UserInfo.builder()
						.id(TO_ACCOUNT_ID)
						.isBlocked(false)
						.build()
		));
		when(transactionRepository.save(any())).thenAnswer(v -> Mono.just(v.getArgument(0)));
		performPaymentAndVerifyStatus(TransactionStatus.SENDER_BLOCKED);
		verifyTransactionWasSavedWithStatus(TransactionStatus.SENDER_BLOCKED);
	}

	@Test
	void shouldReturnStatusReceiverBlockedIfReceiverBlocked() {
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Mono.empty());
		when(userBalanceRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserBalance.builder()
						.userId(FROM_ACCOUNT_ID)
						.balance(TRANSACTION_AMOUNT.add(BigDecimal.TEN))
						.build(),
				UserBalance.builder()
						.userId(TO_ACCOUNT_ID)
						.balance(BigDecimal.TEN)
						.build()
		));
		when(userInfoRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserInfo.builder()
						.id(FROM_ACCOUNT_ID)
						.isBlocked(false)
						.build(),
				UserInfo.builder()
						.id(TO_ACCOUNT_ID)
						.isBlocked(true)
						.build()
		));
		when(transactionRepository.save(any())).thenAnswer(v -> Mono.just(v.getArgument(0)));
		performPaymentAndVerifyStatus(TransactionStatus.RECEIVER_BLOCKED);
		verifyTransactionWasSavedWithStatus(TransactionStatus.RECEIVER_BLOCKED);
	}

	@Test
	void shouldExecuteTransactionIfBalanceIsSufficientAndNoneOfParticipantsBlocked() {
		UserBalance fromBalance = UserBalance.builder()
				.userId(FROM_ACCOUNT_ID)
				.balance(TRANSACTION_AMOUNT.add(BigDecimal.TEN))
				.build();
		UserBalance toBalance = UserBalance.builder()
				.userId(TO_ACCOUNT_ID)
				.balance(BigDecimal.TEN)
				.build();
		when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Mono.empty());
		when(userBalanceRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID)))
				.thenReturn(Flux.just(fromBalance, toBalance));
		when(userInfoRepository.findAllById(List.of(FROM_ACCOUNT_ID, TO_ACCOUNT_ID))).thenReturn(Flux.just(
				UserInfo.builder()
						.id(FROM_ACCOUNT_ID)
						.isBlocked(false)
						.build(),
				UserInfo.builder()
						.id(TO_ACCOUNT_ID)
						.isBlocked(false)
						.build()
		));
		when(userBalanceRepository.saveAll(List.of(fromBalance, toBalance))).thenReturn(Flux.just(fromBalance, toBalance));

		when(transactionRepository.save(any())).thenAnswer(v -> Mono.just(v.getArgument(0)));
		performPaymentAndVerifyStatus(TransactionStatus.SUCCESS);
		verifyTransactionWasSavedWithStatus(TransactionStatus.SUCCESS);
		assertThat(fromBalance.getBalance()).isEqualTo(BigDecimal.TEN);
		assertThat(toBalance.getBalance()).isEqualTo(BigDecimal.TEN.add(TRANSACTION_AMOUNT));
	}

	private void verifyTransactionWasSavedWithStatus(TransactionStatus expectedStatus) {
		verify(transactionRepository).save(new Transaction(TRANSACTION_ID, expectedStatus));
	}

	private void performPaymentAndVerifyStatus(TransactionStatus expectedStatus) {
		StepVerifier.create(paymentService.performPayment(PAYMENT_REQUEST))
				.expectNext(expectedStatus)
				.expectComplete()
				.log()
				.verify();
	}

}
