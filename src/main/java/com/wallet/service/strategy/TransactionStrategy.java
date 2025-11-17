package com.wallet.service.strategy;

import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;

public interface TransactionStrategy {

  TransactionType getType();

  void validate(TransactionRequestContext context);

  void applyOnCreate(TransactionRequestContext context, TransactionStatus status);

  void applyStatusChange(Wallet wallet, WalletTransaction transaction, TransactionStatus newStatus);
}
