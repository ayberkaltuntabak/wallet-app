package com.wallet.service.strategy;

import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.exception.InsufficientBalanceException;
import com.wallet.exception.InvalidTransactionStatusException;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class WithdrawTransactionStrategy implements TransactionStrategy {

  @Override
  public TransactionType getType() {
    return TransactionType.WITHDRAW;
  }

  @Override
  public void validate(TransactionRequestContext context) {
    Wallet wallet = context.wallet();
    if (!wallet.isActiveForWithdraw()) {
      throw new InvalidTransactionStatusException("Wallet not enabled for withdraw");
    }
    if (context.oppositePartyType() == OppositePartyType.PAYMENT && !wallet.isActiveForShopping()) {
      throw new InvalidTransactionStatusException("Wallet not enabled for shopping payments");
    }
    if (wallet.getUsableBalance().compareTo(context.amount()) < 0) {
      throw new InsufficientBalanceException();
    }
  }

  @Override
  public void applyOnCreate(TransactionRequestContext context, TransactionStatus status) {
    Wallet wallet = context.wallet();
    wallet.setUsableBalance(wallet.getUsableBalance().subtract(context.amount()));
    if (status == TransactionStatus.APPROVED) {
      wallet.setBalance(wallet.getBalance().subtract(context.amount()));
    }
  }

  @Override
  public void applyStatusChange(
      Wallet wallet, WalletTransaction transaction, TransactionStatus newStatus) {
    if (newStatus == TransactionStatus.APPROVED) {
      wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount()));
    } else {
      wallet.setUsableBalance(wallet.getUsableBalance().add(transaction.getAmount()));
    }
  }
}
