package com.wallet.dto.request;

import com.wallet.enums.OppositePartyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DepositRequest(
    @NotNull Long walletId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank String source,
    @NotNull OppositePartyType sourceType) {}
