package com.wallet.service.strategy;

import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;
import org.springframework.stereotype.Component;

@Component
public class DepositTransactionStrategy implements TransactionStrategy {

  @Override
  public TransactionType getType() {
    return TransactionType.DEPOSIT;
  }

  @Override
  public void validate(TransactionRequestContext context) {
    // no deposit-specific validation currently
  }

  @Override
  public void applyOnCreate(TransactionRequestContext context, TransactionStatus status) {
    Wallet wallet = context.wallet();
    wallet.setBalance(wallet.getBalance().add(context.amount()));
    if (status == TransactionStatus.APPROVED) {
      wallet.setUsableBalance(wallet.getUsableBalance().add(context.amount()));
    }
  }

  @Override
  public void applyStatusChange(
      Wallet wallet, WalletTransaction transaction, TransactionStatus newStatus) {
    if (newStatus == TransactionStatus.APPROVED) {
      wallet.setUsableBalance(wallet.getUsableBalance().add(transaction.getAmount()));
    } else {
      wallet.setBalance(wallet.getBalance().subtract(transaction.getAmount()));
    }
  }
}
