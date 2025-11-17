package com.wallet.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import com.wallet.model.WalletTransaction;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DepositTransactionStrategyTest {

  private DepositTransactionStrategy strategy;
  private Wallet wallet;
  private TransactionRequestContext context;

  @BeforeEach
  void setup() {
    strategy = new DepositTransactionStrategy();
    wallet = new Wallet();
    Customer customer = new Customer();
    customer.setId(1L);
    wallet.setCustomer(customer);
    wallet.setBalance(BigDecimal.valueOf(1000));
    wallet.setUsableBalance(BigDecimal.valueOf(800));
    context =
        new TransactionRequestContext(
            wallet, BigDecimal.valueOf(200), OppositePartyType.IBAN, "TR123", false);
  }

  @Test
  void applyOnCreateApprovesDeposit() {
    strategy.applyOnCreate(context, TransactionStatus.APPROVED);
    assertThat(wallet.getBalance()).isEqualByComparingTo("1200");
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void applyOnCreatePendingDeposit() {
    strategy.applyOnCreate(context, TransactionStatus.PENDING);
    assertThat(wallet.getBalance()).isEqualByComparingTo("1200");
    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("800");
  }

  @Test
  void applyStatusChangeOnApprovalAddsToUsable() {
    WalletTransaction tx = new WalletTransaction();
    tx.setWallet(wallet);
    tx.setAmount(BigDecimal.valueOf(500));
    tx.setType(TransactionType.DEPOSIT);

    strategy.applyStatusChange(wallet, tx, TransactionStatus.APPROVED);

    assertThat(wallet.getUsableBalance()).isEqualByComparingTo("1300");
  }

  @Test
  void applyStatusChangeOnDenialRollsBackBalance() {
    WalletTransaction tx = new WalletTransaction();
    tx.setWallet(wallet);
    tx.setAmount(BigDecimal.valueOf(500));
    tx.setType(TransactionType.DEPOSIT);

    strategy.applyStatusChange(wallet, tx, TransactionStatus.DENIED);

    assertThat(wallet.getBalance()).isEqualByComparingTo("500");
  }
}
