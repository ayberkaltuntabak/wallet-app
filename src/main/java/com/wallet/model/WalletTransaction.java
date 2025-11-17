package com.wallet.model;

import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class WalletTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  @Column(nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = "opposite_party_type")
  private OppositePartyType oppositePartyType;

  @Column(nullable = false, name = "opposite_party")
  private String oppositeParty;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime processedAt;

  private Long processedBy;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
