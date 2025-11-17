package com.wallet.service;

import com.wallet.dto.response.CustomerResponse;
import com.wallet.exception.CustomerNotFoundException;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.repository.CustomerRepository;
import com.wallet.service.policy.WalletAccessPolicy;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final CurrentCustomerService currentCustomerService;
  private final WalletAccessPolicy walletAccessPolicy;

  public CustomerService(
      CustomerRepository customerRepository,
      CurrentCustomerService currentCustomerService,
      WalletAccessPolicy walletAccessPolicy) {
    this.customerRepository = customerRepository;
    this.currentCustomerService = currentCustomerService;
    this.walletAccessPolicy = walletAccessPolicy;
  }

  public CustomerResponse getCurrentCustomerProfile() {
    Customer current = currentCustomerService.getCurrentCustomer();
    return toResponse(current);
  }

  public List<CustomerResponse> getCustomers() {
    Customer current = currentCustomerService.getCurrentCustomer();
    if (!walletAccessPolicy.canViewAll(current)) {
      throw new UnauthorizedOperationException("Only employees can list customers");
    }
    return customerRepository.findAll().stream().map(this::toResponse).toList();
  }

  public Customer ensureCustomerExists(Long customerId) {
    return customerRepository
        .findById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(customerId));
  }

  private CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getName(),
        customer.getSurname(),
        customer.getTckn(),
        customer.getRole());
  }
}
