package com.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank String name,
    @NotBlank String surname,
    @Pattern(regexp = "\\d{11}", message = "TCKN must be 11 digits") String tckn,
    @Size(min = 8, max = 128) String password) {}
