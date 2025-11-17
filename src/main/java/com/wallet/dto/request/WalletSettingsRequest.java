package com.wallet.dto.request;

import jakarta.validation.constraints.NotNull;

public record WalletSettingsRequest(
    @NotNull Boolean activeForShopping, @NotNull Boolean activeForWithdraw) {}
