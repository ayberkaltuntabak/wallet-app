package com.wallet.dto.response;

import com.wallet.enums.UserRole;

public record CustomerResponse(Long id, String name, String surname, String tckn, UserRole role) {}
