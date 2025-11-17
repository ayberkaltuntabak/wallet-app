package com.wallet.dto.request;

import com.wallet.enums.Currency;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WalletCreateRequest(
    @NotBlank String walletName,
    @NotNull Currency currency,
    @NotNull Boolean activeForShopping,
    @NotNull Boolean activeForWithdraw,
    @Positive @Nullable Long customerId) {}
