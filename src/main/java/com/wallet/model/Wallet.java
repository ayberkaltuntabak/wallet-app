package com.wallet.model;

import com.wallet.enums.Currency;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wallets")
@Getter
@Setter
public class Wallet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(nullable = false)
  private String walletName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Currency currency;

  @Column(nullable = false)
  private boolean activeForShopping;

  @Column(nullable = false)
  private boolean activeForWithdraw;

  @Column(nullable = false)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal usableBalance = BigDecimal.ZERO;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Version private Long version;

  @PrePersist
  public void prePersist() {
    if (balance == null) {
      balance = BigDecimal.ZERO;
    }
    if (usableBalance == null) {
      usableBalance = BigDecimal.ZERO;
    }
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
