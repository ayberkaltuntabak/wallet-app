package com.wallet.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.exception.InsufficientBalanceException;
import com.wallet.exception.InvalidTransactionStatusException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithdrawTransactionStrategyTest {

  private WithdrawTransactionStrategy strategy;
  private Wallet wallet;

  @BeforeEach
  void setup() {
    strategy = new WithdrawTransactionStrategy();
    wallet = new Wallet();
    Customer customer = new Customer();
    customer.setId(1L);
    wallet.setCustomer(customer);
    wallet.setBalance(BigDecimal.valueOf(1000));
    wallet.setUsableBalance(BigDecimal.valueOf(800));
    wallet.setActiveForWithdraw(true);
    wallet.setActiveForShopping(true);
  }

  @Test
  void validateRejectsInactiveWithdraw() {
    wallet.setActiveForWithdraw(false);
    TransactionRequestContext context =
        new TransactionRequestContext(wallet, BigDecimal.TEN, OppositePartyType.IBAN, "TR", false);
    assertThrows(InvalidTransactionStatusException.class, () -> strategy.validate(context));
  }

  @Test
  void validateRejectsShoppingWhenDisabled() {
    wallet.setActiveForShopping(false);
    TransactionRequestContext context =
        new TransactionRequestContext(
            wallet, BigDecimal.TEN, OppositePartyType.PAYMENT, "PAY", true);
    assertThrows(InvalidTransactionStatusException.class, () -> strategy.validate(context));
  }

  @Test
  void validateRejectsInsufficientUsableBalance() {
    TransactionRequestContext context =
        new TransactionRequestContext(
            wallet, BigDecimal.valueOf(900), OppositePartyType.IBAN, "TR", false);
    assertThrows(InsufficientBalanceException.class, () -> strategy.validate(context));
  }

  @Test
  void applyOnCreateAdjustsBalances() {
    TransactionRequestContext context =
        new TransactionRequestContext(
            wallet, BigDecimal.valueOf(100), OppositePartyType.IBAN, "TR", false);
    strategy.applyOnCreate(context, TransactionStatus.PENDING);
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("700");
    assertThat(wallet.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void applyOnCreateApprovedWithdrawUpdatesBalance() {
    TransactionRequestContext context =
        new TransactionRequestContext(
            wallet, BigDecimal.valueOf(100), OppositePartyType.IBAN, "TR", false);
    strategy.applyOnCreate(context, TransactionStatus.APPROVED);
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("700");
    assertThat(wallet.getBalance()).isEqualByComparingTo("900");
  }

  @Test
  void applyStatusChangeOnApprovalSubtractsBalance() {
    WalletTransaction tx = new WalletTransaction();
    tx.setWallet(wallet);
    tx.setAmount(BigDecimal.valueOf(200));
    tx.setType(TransactionType.WITHDRAW);

    strategy.applyStatusChange(wallet, tx, TransactionStatus.APPROVED);

    assertThat(wallet.getBalance()).isEqualByComparingTo("800");
  }

  @Test
  void applyStatusChangeOnDenialRestoresUsableBalance() {
    wallet.setUsableBalance(BigDecimal.valueOf(300));
    WalletTransaction tx = new WalletTransaction();
    tx.setWallet(wallet);
    tx.setAmount(BigDecimal.valueOf(200));
    tx.setType(TransactionType.WITHDRAW);

    strategy.applyStatusChange(wallet, tx, TransactionStatus.DENIED);

    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("500");
  }
}
