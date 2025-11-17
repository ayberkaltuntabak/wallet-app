package com.wallet.dto.request;

import com.wallet.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public record ApproveTransactionRequest(@NotNull TransactionStatus status) {}
