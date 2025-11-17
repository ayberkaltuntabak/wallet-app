package com.wallet.service;

import com.wallet.dto.request.ApproveTransactionRequest;
import com.wallet.dto.request.DepositRequest;
import com.wallet.dto.request.WithdrawRequest;
import com.wallet.dto.response.TransactionResponse;
import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.exception.InvalidTransactionStatusException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.WalletTransactionRepository;
import com.wallet.service.strategy.TransactionRequestContext;
import com.wallet.service.strategy.TransactionStrategy;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Coordinates the lifecycle of deposits/withdraws but delegates wallet math to strategies. This
 * keeps the service readable and makes it easy to bolt on new transaction types later.
 */
@Service
public class TransactionService {

  private static final BigDecimal APPROVAL_THRESHOLD = BigDecimal.valueOf(1000);

  private final WalletService walletService;
  private final WalletRepository walletRepository;
  private final WalletTransactionRepository transactionRepository;
  private final CurrentCustomerService currentCustomerService;
  private final Map<TransactionType, TransactionStrategy> strategies;

  public TransactionService(
      WalletService walletService,
      WalletRepository walletRepository,
      WalletTransactionRepository transactionRepository,
      CurrentCustomerService currentCustomerService,
      List<TransactionStrategy> strategies) {
    this.walletService = walletService;
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.currentCustomerService = currentCustomerService;
    this.strategies =
        strategies.stream()
            .collect(
                Collectors.toUnmodifiableMap(TransactionStrategy::getType, Function.identity()));
  }

  @Transactional
  public TransactionResponse deposit(DepositRequest request) {
    TransactionStrategy strategy = strategyFor(TransactionType.DEPOSIT);
    Wallet wallet = walletService.requireWalletAccess(request.walletId());
    TransactionStatus status = determineStatus(request.amount());
    TransactionRequestContext context =
        new TransactionRequestContext(
            wallet, request.amount(), request.sourceType(), request.source(), false);
    strategy.validate(context);
    strategy.applyOnCreate(context, status);
    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setAmount(request.amount());
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setStatus(status);
    transaction.setOppositeParty(request.source());
    transaction.setOppositePartyType(request.sourceType());
    if (status == TransactionStatus.APPROVED) {
      transaction.setProcessedAt(LocalDateTime.now());
      transaction.setProcessedBy(wallet.getCustomer().getId());
    }
    walletRepository.save(wallet);
    WalletTransaction saved = transactionRepository.save(transaction);
    return toResponse(saved);
  }

  @Transactional
  public TransactionResponse withdraw(WithdrawRequest request) {
    TransactionStrategy strategy = strategyFor(TransactionType.WITHDRAW);
    Wallet wallet = walletService.requireWalletAccess(request.walletId());
    TransactionStatus status = determineStatus(request.amount());
    TransactionRequestContext context =
        new TransactionRequestContext(
            wallet,
            request.amount(),
            request.destinationType(),
            request.destination(),
            request.destinationType() == OppositePartyType.PAYMENT);
    strategy.validate(context);
    strategy.applyOnCreate(context, status);
    WalletTransaction transaction = new WalletTransaction();
    transaction.setWallet(wallet);
    transaction.setAmount(request.amount());
    transaction.setType(TransactionType.WITHDRAW);
    transaction.setStatus(status);
    transaction.setOppositeParty(request.destination());
    transaction.setOppositePartyType(request.destinationType());
    if (status == TransactionStatus.APPROVED) {
      transaction.setProcessedAt(LocalDateTime.now());
      transaction.setProcessedBy(wallet.getCustomer().getId());
    }
    walletRepository.save(wallet);
    WalletTransaction saved = transactionRepository.save(transaction);
    return toResponse(saved);
  }

  public List<TransactionResponse> listTransactions(Long walletId) {
    walletService.requireWalletAccess(walletId);
    return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId).stream()
        .map(this::toResponse)
        .toList();
  }

  public TransactionResponse getTransaction(Long transactionId) {
    WalletTransaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new InvalidTransactionStatusException("Transaction not found"));
    walletService.requireWalletAccess(transaction.getWallet().getId());
    return toResponse(transaction);
  }

  @Transactional
  public TransactionResponse approveOrDeny(Long transactionId, ApproveTransactionRequest request) {
    if (request.status() == TransactionStatus.PENDING) {
      throw new InvalidTransactionStatusException("Status must be APPROVED or DENIED");
    }
    WalletTransaction transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(() -> new InvalidTransactionStatusException("Transaction not found"));
    if (transaction.getStatus() != TransactionStatus.PENDING) {
      throw new InvalidTransactionStatusException("Only pending transactions can be updated");
    }
    Wallet wallet = transaction.getWallet();
    Customer actor = currentCustomerService.getCurrentCustomer();
    TransactionStrategy strategy = strategyFor(transaction.getType());
    strategy.applyStatusChange(wallet, transaction, request.status());
    transaction.setStatus(request.status());
    transaction.setProcessedAt(LocalDateTime.now());
    transaction.setProcessedBy(actor.getId());
    walletRepository.save(wallet);
    WalletTransaction saved = transactionRepository.save(transaction);
    return toResponse(saved);
  }

  private TransactionStatus determineStatus(BigDecimal amount) {
    return amount.compareTo(APPROVAL_THRESHOLD) > 0
        ? TransactionStatus.PENDING
        : TransactionStatus.APPROVED;
  }

  private TransactionStrategy strategyFor(TransactionType type) {
    TransactionStrategy strategy = strategies.get(type);
    if (strategy == null) {
      throw new IllegalStateException("No strategy registered for type " + type);
    }
    return strategy;
  }

  private TransactionResponse toResponse(WalletTransaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getWallet().getId(),
        transaction.getType(),
        transaction.getStatus(),
        transaction.getAmount(),
        transaction.getOppositePartyType(),
        transaction.getOppositeParty(),
        transaction.getCreatedAt(),
        transaction.getProcessedAt(),
        transaction.getProcessedBy());
  }
}
