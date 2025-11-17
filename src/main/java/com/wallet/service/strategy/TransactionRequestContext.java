package com.wallet.service.strategy;

import com.wallet.enums.OppositePartyType;
import com.wallet.model.Wallet;
import java.math.BigDecimal;

public record TransactionRequestContext(
    Wallet wallet,
    BigDecimal amount,
    OppositePartyType oppositePartyType,
    String oppositeParty,
    boolean shoppingPayment) {}
