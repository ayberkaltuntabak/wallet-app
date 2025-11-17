package com.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @Pattern(regexp = "\\d{11}", message = "TCKN must be 11 digits") String tckn,
    @NotBlank String password) {}
