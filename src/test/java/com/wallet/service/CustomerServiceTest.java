package com.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.wallet.dto.response.CustomerResponse;
import com.wallet.enums.UserRole;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.repository.CustomerRepository;
import com.wallet.service.policy.WalletAccessPolicy;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private CurrentCustomerService currentCustomerService;
  @Mock private WalletAccessPolicy walletAccessPolicy;

  @InjectMocks private CustomerService customerService;

  private Customer customer;

  @BeforeEach
  void setup() {
    customer = new Customer();
    customer.setId(5L);
    customer.setName("Mert");
    customer.setSurname("Demir");
    customer.setTckn("10000000012");
    customer.setRole(UserRole.CUSTOMER);
  }

  @Test
  void getCurrentProfileDelegatesToMapper() {
    when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);

    CustomerResponse response = customerService.getCurrentCustomerProfile();

    assertThat(response.id()).isEqualTo(5L);
    assertThat(response.name()).isEqualTo("Mert");
  }

  @Test
  void getCustomersRequiresEmployeeRole() {
    when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
    when(walletAccessPolicy.canViewAll(customer)).thenReturn(false);

    assertThrows(UnauthorizedOperationException.class, () -> customerService.getCustomers());
  }

  @Test
  void getCustomersReturnsAllWhenAuthorized() {
    Customer employee = new Customer();
    employee.setId(1L);
    employee.setRole(UserRole.EMPLOYEE);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(employee);
    when(walletAccessPolicy.canViewAll(employee)).thenReturn(true);
    when(customerRepository.findAll()).thenReturn(List.of(customer));

    List<CustomerResponse> responses = customerService.getCustomers();

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(customer.getId());
  }
}
