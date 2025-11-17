package com.wallet.service.policy;

import com.wallet.enums.UserRole;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import org.springframework.stereotype.Component;

/**
 * Centralizes all wallet-visibility rules so we don't scatter role checks around the codebase. When
 * new roles arrive (auditors, finance, etc.) this is the single place to reason about them.
 */
@Component
public class WalletAccessPolicy {

  public void ensureCanAccess(Customer actor, Wallet wallet) {
    if (actor.getRole() == UserRole.CUSTOMER
        && !wallet.getCustomer().getId().equals(actor.getId())) {
      throw new UnauthorizedOperationException("Cannot access other customer's wallet");
    }
  }

  public boolean canViewAll(Customer actor) {
    return actor.getRole() == UserRole.EMPLOYEE;
  }
}
