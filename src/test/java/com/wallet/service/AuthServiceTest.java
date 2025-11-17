package com.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wallet.dto.request.LoginRequest;
import com.wallet.dto.request.RegisterRequest;
import com.wallet.dto.response.AuthResponse;
import com.wallet.dto.response.CustomerResponse;
import com.wallet.enums.UserRole;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.model.Customer;
import com.wallet.repository.CustomerRepository;
import com.wallet.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtTokenProvider jwtTokenProvider;

  @InjectMocks private AuthService authService;

  private RegisterRequest registerRequest;

  @BeforeEach
  void setup() {
    registerRequest = new RegisterRequest("Ada", "Lovelace", "12345678901", "Password123!");
  }

  @Test
  void registerPersistsCustomerWithEncodedPassword() {
    when(customerRepository.findByTckn("12345678901")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("Password123!")).thenReturn("encoded");
    Customer saved = new Customer();
    saved.setId(1L);
    saved.setName("Ada");
    saved.setSurname("Lovelace");
    saved.setTckn("12345678901");
    saved.setRole(UserRole.CUSTOMER);
    when(customerRepository.save(any(Customer.class))).thenReturn(saved);

    CustomerResponse response = authService.register(registerRequest);

    assertThat(response.id()).isEqualTo(1L);
    ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
    verify(customerRepository).save(captor.capture());
    assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
  }

  @Test
  void registerFailsWhenDuplicateTckn() {
    when(customerRepository.findByTckn("12345678901")).thenReturn(Optional.of(new Customer()));
    assertThrows(UnauthorizedOperationException.class, () -> authService.register(registerRequest));
  }

  @Test
  void loginReturnsTokens() {
    LoginRequest request = new LoginRequest("12345678901", "Password123!");
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("12345678901", "Password123!");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("access");
    when(jwtTokenProvider.generateRefreshToken(authentication)).thenReturn("refresh");

    AuthResponse response = authService.login(request);

    assertThat(response.accessToken()).isEqualTo("access");
    assertThat(response.refreshToken()).isEqualTo("refresh");
  }
}
