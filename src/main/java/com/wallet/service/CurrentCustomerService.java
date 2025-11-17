package com.wallet.service;

import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class CurrentCustomerService {

  private final CustomerRepository customerRepository;

  public CurrentCustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public Customer getCurrentCustomer() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
      throw new UnauthorizedOperationException("No authenticated customer");
    }
    return customerRepository
        .findByTckn(userDetails.getUsername())
        .orElseThrow(() -> new UnauthorizedOperationException("Customer not found"));
  }
}
