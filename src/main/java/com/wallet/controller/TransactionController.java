package com.wallet.controller;

import com.wallet.dto.request.ApproveTransactionRequest;
import com.wallet.dto.request.DepositRequest;
import com.wallet.dto.request.WithdrawRequest;
import com.wallet.dto.response.TransactionResponse;
import com.wallet.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

  private final TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping("/deposit")
  public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(transactionService.deposit(request));
  }

  @PostMapping("/withdraw")
  public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(transactionService.withdraw(request));
  }

  @GetMapping
  public ResponseEntity<List<TransactionResponse>> list(@RequestParam("walletId") Long walletId) {
    return ResponseEntity.ok(transactionService.listTransactions(walletId));
  }

  @GetMapping("/{transactionId}")
  public ResponseEntity<TransactionResponse> get(
      @PathVariable("transactionId") Long transactionId) {
    return ResponseEntity.ok(transactionService.getTransaction(transactionId));
  }

  @PostMapping("/{transactionId}/approve")
  @PreAuthorize("hasAuthority('EMPLOYEE')")
  public ResponseEntity<TransactionResponse> approve(
      @PathVariable("transactionId") Long transactionId,
      @Valid @RequestBody ApproveTransactionRequest request) {
    return ResponseEntity.ok(transactionService.approveOrDeny(transactionId, request));
  }
}
