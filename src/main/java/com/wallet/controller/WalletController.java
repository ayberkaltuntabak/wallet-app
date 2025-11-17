package com.wallet.controller;

import com.wallet.dto.request.WalletCreateRequest;
import com.wallet.dto.request.WalletSettingsRequest;
import com.wallet.dto.response.WalletResponse;
import com.wallet.enums.Currency;
import com.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @PostMapping
  public ResponseEntity<WalletResponse> create(@Valid @RequestBody WalletCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(request));
  }

  @GetMapping
  public ResponseEntity<List<WalletResponse>> list(
      @RequestParam(name = "customerId", required = false) Long customerId,
      @RequestParam(name = "currency", required = false) Currency currency) {
    return ResponseEntity.ok(
        walletService.listWallets(Optional.ofNullable(customerId), Optional.ofNullable(currency)));
  }

  @GetMapping("/{walletId}")
  public ResponseEntity<WalletResponse> get(@PathVariable("walletId") Long walletId) {
    return ResponseEntity.ok(walletService.getWallet(walletId));
  }

  @PutMapping("/{walletId}/settings")
  public ResponseEntity<WalletResponse> update(
      @PathVariable("walletId") Long walletId, @Valid @RequestBody WalletSettingsRequest request) {
    return ResponseEntity.ok(walletService.updateSettings(walletId, request));
  }
}
