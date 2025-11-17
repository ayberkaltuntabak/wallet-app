package com.wallet.dto.response;

import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionStatus;
import com.wallet.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    Long walletId,
    TransactionType type,
    TransactionStatus status,
    BigDecimal amount,
    OppositePartyType oppositePartyType,
    String oppositeParty,
    LocalDateTime createdAt,
    LocalDateTime processedAt,
    Long processedBy) {}
