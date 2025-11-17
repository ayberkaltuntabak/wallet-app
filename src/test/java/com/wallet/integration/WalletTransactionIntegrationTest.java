package com.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.enums.OppositePartyType;
import com.wallet.enums.TransactionAuditAction;
import com.wallet.enums.TransactionStatus;
import com.wallet.model.TransactionAuditLog;
import com.wallet.repository.TransactionAuditLogRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class WalletTransactionIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TransactionAuditLogRepository auditLogRepository;

  @BeforeEach
  void cleanAuditLogs() {
    auditLogRepository.deleteAll();
  }

  @Test
  void depositAndWithdrawFlow() throws Exception {
    String token = login("10000000012", "Customer123!");

    Long walletId = createWallet(token, "Integration-" + System.nanoTime(), false, true);

    Long approvedTransactionId =
        deposit(token, walletId, BigDecimal.valueOf(250), TransactionStatus.APPROVED);

    assertAuditLog(approvedTransactionId, TransactionAuditAction.DEPOSIT_CREATED, 1);

    mockMvc
        .perform(
            get("/api/v1/transactions")
                .param("walletId", walletId.toString())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("APPROVED"));

    Map<String, Object> withdrawBody = new HashMap<>();
    withdrawBody.put("walletId", walletId);
    withdrawBody.put("amount", 50);
    withdrawBody.put("destination", "PAY-123");
    withdrawBody.put("destinationType", OppositePartyType.PAYMENT.name());

    mockMvc
        .perform(
            post("/api/v1/transactions/withdraw")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawBody)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Wallet not enabled for shopping payments"));
  }

  @Test
  void employeeCanDenyPendingTransactions() throws Exception {
    String customerToken = login("10000000012", "Customer123!");
    String employeeToken = login("10000000001", "Password123!");

    Long walletId = createWallet(customerToken, "Approval-" + System.nanoTime(), true, true);
    Long transactionId =
        deposit(customerToken, walletId, BigDecimal.valueOf(1500), TransactionStatus.PENDING);

    assertWalletBalances(customerToken, walletId, 1500, 0);
    assertAuditLog(transactionId, TransactionAuditAction.DEPOSIT_CREATED, 1);

    Map<String, Object> requestBody = Map.of("status", TransactionStatus.DENIED.name());

    mockMvc
        .perform(
            post("/api/v1/transactions/{transactionId}", transactionId)
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(TransactionStatus.DENIED.name()));

    assertWalletBalances(customerToken, walletId, 0, 0);
    assertAuditLog(transactionId, TransactionAuditAction.STATUS_CHANGED, 2);
  }

  private String login(String tckn, String password) throws Exception {
    Map<String, Object> body = Map.of("tckn", tckn, "password", password);
    // TODO: when refresh token endpoints ship, expand this helper to cover refresh flow too.
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("accessToken").asText();
  }

  private Long createWallet(String token, String name, boolean shopping, boolean withdraw)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("walletName", name);
    body.put("currency", "TRY");
    body.put("activeForShopping", shopping);
    body.put("activeForWithdraw", withdraw);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/wallets")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("id").asLong();
  }

  private Long deposit(
      String token, Long walletId, BigDecimal amount, TransactionStatus expectedStatus)
      throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("walletId", walletId);
    body.put("amount", amount);
    body.put("source", "TR123");
    body.put("sourceType", OppositePartyType.IBAN.name());

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/transactions/deposit")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(expectedStatus.name()))
            .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("id").asLong();
  }

  private void assertWalletBalances(String token, Long walletId, int balance, int usableBalance)
      throws Exception {
    mockMvc
        .perform(
            get("/api/v1/wallets/{walletId}", walletId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(balance))
        .andExpect(jsonPath("$.usableBalance").value(usableBalance));
  }

  private void assertAuditLog(
      Long transactionId, TransactionAuditAction lastAction, int expectedCount) {
    List<TransactionAuditLog> logs = auditLogRepository.findByTransactionId(transactionId);
    assertThat(logs).hasSize(expectedCount);
    assertThat(logs.get(logs.size() - 1).getAction()).isEqualTo(lastAction);
  }
}
