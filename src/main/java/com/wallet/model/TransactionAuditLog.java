package com.wallet.model;

import com.wallet.enums.TransactionAuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_audit_logs")
@Getter
@Setter
public class TransactionAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, name = "transaction_id")
  private Long transactionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionAuditAction action;

  @Column(name = "actor_id")
  private Long actorId;

  @Column(nullable = false, length = 500)
  private String details;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
