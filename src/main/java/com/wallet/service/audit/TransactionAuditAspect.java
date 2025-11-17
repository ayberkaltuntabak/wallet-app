package com.wallet.service.audit;

import com.wallet.dto.request.ApproveTransactionRequest;
import com.wallet.dto.request.DepositRequest;
import com.wallet.dto.request.WithdrawRequest;
import com.wallet.dto.response.TransactionResponse;
import com.wallet.enums.TransactionAuditAction;
import com.wallet.model.Customer;
import com.wallet.model.TransactionAuditLog;
import com.wallet.repository.TransactionAuditLogRepository;
import com.wallet.service.CurrentCustomerService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TransactionAuditAspect {

  private final TransactionAuditLogRepository auditLogRepository;
  private final CurrentCustomerService currentCustomerService;

  public TransactionAuditAspect(
      TransactionAuditLogRepository auditLogRepository,
      CurrentCustomerService currentCustomerService) {
    this.auditLogRepository = auditLogRepository;
    this.currentCustomerService = currentCustomerService;
  }

  @AfterReturning(
      value = "execution(* com.wallet.service.TransactionService.deposit(..))",
      returning = "response")
  public void logDeposit(JoinPoint joinPoint, TransactionResponse response) {
    DepositRequest request = (DepositRequest) joinPoint.getArgs()[0];
    String details =
        String.format(
            "amount=%s, sourceType=%s, source=%s",
            request.amount(), request.sourceType(), request.source());
    persistLog(response.id(), TransactionAuditAction.DEPOSIT_CREATED, details);
  }

  @AfterReturning(
      value = "execution(* com.wallet.service.TransactionService.withdraw(..))",
      returning = "response")
  public void logWithdraw(JoinPoint joinPoint, TransactionResponse response) {
    WithdrawRequest request = (WithdrawRequest) joinPoint.getArgs()[0];
    String details =
        String.format(
            "amount=%s, destinationType=%s, destination=%s",
            request.amount(), request.destinationType(), request.destination());
    persistLog(response.id(), TransactionAuditAction.WITHDRAW_CREATED, details);
  }

  @AfterReturning(
      value = "execution(* com.wallet.service.TransactionService.approveOrDeny(..))",
      returning = "response")
  public void logStatusChange(JoinPoint joinPoint, TransactionResponse response) {
    ApproveTransactionRequest request = (ApproveTransactionRequest) joinPoint.getArgs()[1];
    String details = "status=" + request.status().name();
    persistLog(response.id(), TransactionAuditAction.STATUS_CHANGED, details);
  }

  private void persistLog(Long transactionId, TransactionAuditAction action, String details) {
    TransactionAuditLog log = new TransactionAuditLog();
    log.setTransactionId(transactionId);
    log.setAction(action);
    log.setDetails(details);
    Customer actor = null;
    try {
      actor = currentCustomerService.getCurrentCustomer();
    } catch (Exception ignored) {
      // Fallback to null actor (system-driven calls)
    }
    if (actor != null) {
      log.setActorId(actor.getId());
    }
    auditLogRepository.save(log);
  }
}
