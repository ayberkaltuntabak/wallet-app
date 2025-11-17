package com.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wallet.dto.request.ApproveTransactionRequest;
import com.wallet.dto.request.DepositRequest;
import com.wallet.dto.request.WithdrawRequest;
import com.wallet.dto.response.TransactionResponse;
import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.exception.InsufficientBalanceException;
import com.wallet.exception.InvalidTransactionStatusException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.WalletTransactionRepository;
import com.wallet.service.strategy.DepositTransactionStrategy;
import com.wallet.service.strategy.TransactionStrategy;
import com.wallet.service.strategy.WithdrawTransactionStrategy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @Mock private WalletService walletService;
  @Mock private WalletRepository walletRepository;
  @Mock private WalletTransactionRepository transactionRepository;
  @Mock private CurrentCustomerService currentCustomerService;

  private TransactionService transactionService;

  private Wallet wallet;

  @BeforeEach
  void setup() {
    wallet = new Wallet();
    Customer customer = new Customer();
    customer.setId(10L);
    wallet.setCustomer(customer);
    wallet.setBalance(BigDecimal.valueOf(1000));
    wallet.setUsableBalance(BigDecimal.valueOf(1000));
    wallet.setActiveForWithdraw(true);
    List<TransactionStrategy> strategies =
        List.of(new DepositTransactionStrategy(), new WithdrawTransactionStrategy());
    transactionService =
        new TransactionService(
            walletService,
            walletRepository,
            transactionRepository,
            currentCustomerService,
            strategies);
  }

  @Test
  void depositBelowThresholdApprovesAndUpdatesBalances() {
    DepositRequest request =
        new DepositRequest(1L, BigDecimal.valueOf(500), "TR12", OppositePartyType.IBAN);
    WalletTransaction persisted = new WalletTransaction();
    persisted.setId(5L);
    persisted.setWallet(wallet);
    persisted.setAmount(request.amount());
    persisted.setType(TransactionType.DEPOSIT);
    persisted.setStatus(TransactionStatus.APPROVED);

    when(walletService.requireWalletAccess(1L)).thenReturn(wallet);
    when(transactionRepository.save(any())).thenReturn(persisted);

    TransactionResponse response = transactionService.deposit(request);

    assertThat(response.status()).isEqualTo(TransactionStatus.APPROVED);
    assertThat(wallet.getBalance()).isEqualByComparingTo("1500");
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("1500");
    verify(walletRepository).save(wallet);
  }

  @Test
  void withdrawThrowsWhenWalletDisabled() {
    wallet.setActiveForWithdraw(false);
    WithdrawRequest request =
        new WithdrawRequest(1L, BigDecimal.valueOf(100), "TRxx", OppositePartyType.IBAN);
    when(walletService.requireWalletAccess(1L)).thenReturn(wallet);
    assertThrows(
        InvalidTransactionStatusException.class, () -> transactionService.withdraw(request));
  }

  @Test
  void withdrawThrowsWhenInsufficientUsableBalance() {
    wallet.setUsableBalance(BigDecimal.ZERO);
    WithdrawRequest request =
        new WithdrawRequest(1L, BigDecimal.valueOf(100), "TRxx", OppositePartyType.IBAN);
    when(walletService.requireWalletAccess(1L)).thenReturn(wallet);
    assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(request));
  }

  @Test
  void withdrawPaymentRequiresShoppingFlag() {
    wallet.setActiveForShopping(false);
    WithdrawRequest request =
        new WithdrawRequest(1L, BigDecimal.valueOf(50), "PAY123", OppositePartyType.PAYMENT);
    when(walletService.requireWalletAccess(1L)).thenReturn(wallet);
    assertThrows(
        InvalidTransactionStatusException.class, () -> transactionService.withdraw(request));
  }

  @Test
  void approvePendingDepositMovesAmountsToUsableBalance() {
    WalletTransaction pending = new WalletTransaction();
    pending.setId(99L);
    pending.setWallet(wallet);
    pending.setAmount(BigDecimal.valueOf(2000));
    pending.setType(TransactionType.DEPOSIT);
    pending.setStatus(TransactionStatus.PENDING);
    when(transactionRepository.findById(99L)).thenReturn(Optional.of(pending));
    when(transactionRepository.save(pending)).thenReturn(pending);
    Customer employee = new Customer();
    employee.setId(1L);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(employee);

    TransactionResponse response =
        transactionService.approveOrDeny(
            99L, new ApproveTransactionRequest(TransactionStatus.APPROVED));

    assertThat(response.status()).isEqualTo(TransactionStatus.APPROVED);
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("3000");
    verify(walletRepository).save(wallet);
  }

  @Test
  void denyPendingDepositRollsBackBalance() {
    wallet.setBalance(BigDecimal.valueOf(3000));
    WalletTransaction pending = new WalletTransaction();
    pending.setId(77L);
    pending.setWallet(wallet);
    pending.setAmount(BigDecimal.valueOf(2000));
    pending.setType(TransactionType.DEPOSIT);
    pending.setStatus(TransactionStatus.PENDING);
    when(transactionRepository.findById(77L)).thenReturn(Optional.of(pending));
    when(transactionRepository.save(pending)).thenReturn(pending);
    Customer employee = new Customer();
    employee.setId(5L);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(employee);

    TransactionResponse response =
        transactionService.approveOrDeny(
            77L, new ApproveTransactionRequest(TransactionStatus.DENIED));

    assertThat(response.status()).isEqualTo(TransactionStatus.DENIED);
    assertThat(wallet.getBalance()).isEqualByComparingTo("1000");
    verify(walletRepository).save(wallet);
  }

  @Test
  void denyPendingWithdrawRestoresUsableBalance() {
    wallet.setUsableBalance(BigDecimal.valueOf(300));
    WalletTransaction pending = new WalletTransaction();
    pending.setId(88L);
    pending.setWallet(wallet);
    pending.setAmount(BigDecimal.valueOf(200));
    pending.setType(TransactionType.WITHDRAW);
    pending.setStatus(TransactionStatus.PENDING);
    when(transactionRepository.findById(88L)).thenReturn(Optional.of(pending));
    when(transactionRepository.save(pending)).thenReturn(pending);
    Customer employee = new Customer();
    employee.setId(6L);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(employee);

    TransactionResponse response =
        transactionService.approveOrDeny(
            88L, new ApproveTransactionRequest(TransactionStatus.DENIED));

    assertThat(response.status()).isEqualTo(TransactionStatus.DENIED);
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("500");
    verify(walletRepository).save(wallet);
  }
}
