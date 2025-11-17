package com.wallet.repository;

import com.wallet.enums.Currency;
import com.wallet.model.Wallet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

  List<Wallet> findByCustomerId(Long customerId);

  List<Wallet> findByCustomerIdAndCurrency(Long customerId, Currency currency);
}
