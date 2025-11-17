package com.wallet.dto.response;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
