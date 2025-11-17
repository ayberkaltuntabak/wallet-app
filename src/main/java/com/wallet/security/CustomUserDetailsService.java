package com.wallet.security;

import com.wallet.model.Customer;
import com.wallet.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final CustomerRepository customerRepository;

  public CustomUserDetailsService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Customer customer =
        customerRepository
            .findByTckn(username)
            .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));
    return new CustomUserDetails(customer);
  }
}
