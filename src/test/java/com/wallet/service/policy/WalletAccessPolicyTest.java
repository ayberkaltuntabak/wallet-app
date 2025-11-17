package com.wallet.service.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wallet.enums.UserRole;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalletAccessPolicyTest {

  private WalletAccessPolicy policy;
  private Customer employee;
  private Customer owner;
  private Wallet wallet;

  @BeforeEach
  void setup() {
    policy = new WalletAccessPolicy();
    employee = new Customer();
    employee.setId(1L);
    employee.setRole(UserRole.EMPLOYEE);
    owner = new Customer();
    owner.setId(2L);
    owner.setRole(UserRole.CUSTOMER);
    wallet = new Wallet();
    wallet.setCustomer(owner);
  }

  @Test
  void employeeCanAccessAnyWallet() {
    policy.ensureCanAccess(employee, wallet);
    assertThat(policy.canViewAll(employee)).isTrue();
  }

  @Test
  void customerCanOnlyAccessOwnWallet() {
    policy.ensureCanAccess(owner, wallet);

    Customer other = new Customer();
    other.setId(3L);
    other.setRole(UserRole.CUSTOMER);
    assertThrows(UnauthorizedOperationException.class, () -> policy.ensureCanAccess(other, wallet));
    assertThat(policy.canViewAll(owner)).isFalse();
  }
}
