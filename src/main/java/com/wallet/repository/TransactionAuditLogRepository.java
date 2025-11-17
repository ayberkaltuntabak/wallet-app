package com.wallet.repository;

import com.wallet.model.TransactionAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionAuditLogRepository
    extends JpaRepository<TransactionAuditLog, Long> {

  List<TransactionAuditLog> findByTransactionId(Long transactionId);
}
