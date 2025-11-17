package com.wallet.repository;

import com.wallet.model.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

  List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
