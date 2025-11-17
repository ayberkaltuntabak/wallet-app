package com.wallet.service;

import com.wallet.dto.request.LoginRequest;
import com.wallet.dto.request.RegisterRequest;
import com.wallet.dto.response.AuthResponse;
import com.wallet.dto.response.CustomerResponse;
import com.wallet.enums.UserRole;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.repository.CustomerRepository;
import com.wallet.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;

  public AuthService(
      CustomerRepository customerRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtTokenProvider tokenProvider) {
    this.customerRepository = customerRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.tokenProvider = tokenProvider;
  }

  @Transactional
  public CustomerResponse register(RegisterRequest request) {
    customerRepository
        .findByTckn(request.tckn())
        .ifPresent(
            c -> {
              throw new UnauthorizedOperationException("TCKN already exists");
            });
    Customer customer = new Customer();
    customer.setName(request.name());
    customer.setSurname(request.surname());
    customer.setTckn(request.tckn());
    customer.setPassword(passwordEncoder.encode(request.password()));
    customer.setRole(UserRole.CUSTOMER);
    return toResponse(customerRepository.save(customer));
  }

  public AuthResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.tckn(), request.password()));
    String accessToken = tokenProvider.generateAccessToken(authentication);
    String refreshToken = tokenProvider.generateRefreshToken(authentication);
    return new AuthResponse(accessToken, refreshToken, 900);
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
