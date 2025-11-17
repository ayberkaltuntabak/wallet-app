package com.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wallet.dto.request.WalletCreateRequest;
import com.wallet.dto.request.WalletSettingsRequest;
import com.wallet.dto.response.WalletResponse;
import com.wallet.enums.Currency;
import com.wallet.enums.UserRole;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import com.wallet.repository.WalletRepository;
import com.wallet.service.policy.WalletAccessPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

  @Mock private WalletRepository walletRepository;
  @Mock private CurrentCustomerService currentCustomerService;
  @Mock private WalletAccessPolicy walletAccessPolicy;
  @Mock private CustomerService customerService;

  @InjectMocks private WalletService walletService;

  private Customer customer;
  private Wallet wallet;

  @BeforeEach
  void setup() {
    customer = new Customer();
    customer.setId(10L);
    customer.setRole(UserRole.CUSTOMER);
    customer.setName("Ada");
    customer.setSurname("Lovelace");
    customer.setTckn("12345678901");

    wallet = new Wallet();
    wallet.setId(1L);
    wallet.setCustomer(customer);
    wallet.setWalletName("Daily");
    wallet.setCurrency(Currency.TRY);
    wallet.setActiveForShopping(true);
    wallet.setActiveForWithdraw(true);
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setUsableBalance(BigDecimal.ZERO);
  }

  @Test
  void createWalletUsesCurrentCustomer() {
    WalletCreateRequest request = new WalletCreateRequest("Trips", Currency.EUR, true, false, null);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
    when(walletRepository.save(any(Wallet.class)))
        .thenAnswer(
            invocation -> {
              Wallet saved = invocation.getArgument(0);
              saved.setId(5L);
              return saved;
            });

    WalletResponse response = walletService.createWallet(request);

    assertThat(response.customerId()).isEqualTo(customer.getId());
    assertThat(response.walletName()).isEqualTo("Trips");
    verify(walletRepository).save(any(Wallet.class));
  }

  @Test
  void employeeCanCreateWalletForAnotherCustomer() {
    Customer employee = new Customer();
    employee.setId(1L);
    employee.setRole(UserRole.EMPLOYEE);
    Customer target = new Customer();
    target.setId(99L);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(employee);
    when(customerService.ensureCustomerExists(99L)).thenReturn(target);
    when(walletRepository.save(any(Wallet.class)))
        .thenAnswer(
            invocation -> {
              Wallet saved = invocation.getArgument(0);
              saved.setId(6L);
              return saved;
            });

    WalletCreateRequest request =
        new WalletCreateRequest("Ops Wallet", Currency.USD, true, true, 99L);

    WalletResponse response = walletService.createWallet(request);

    assertThat(response.customerId()).isEqualTo(99L);
    verify(customerService).ensureCustomerExists(99L);
  }

  @Test
  void customerCannotSpoofAnotherCustomerId() {
    WalletCreateRequest request = new WalletCreateRequest("Trips", Currency.EUR, true, false, 25L);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);

    assertThrows(
        com.wallet.exception.UnauthorizedOperationException.class,
        () -> walletService.createWallet(request));
  }

  @Test
  void listWalletsForEmployeeCanFetchAll() {
    Customer employee = new Customer();
    employee.setId(1L);
    employee.setRole(UserRole.EMPLOYEE);
    when(currentCustomerService.getCurrentCustomer()).thenReturn(employee);
    when(walletAccessPolicy.canViewAll(employee)).thenReturn(true);
    when(walletRepository.findAll()).thenReturn(List.of(wallet));

    List<WalletResponse> responses = walletService.listWallets(Optional.empty(), Optional.empty());

    assertThat(responses).hasSize(1);
    verify(walletRepository).findAll();
  }

  @Test
  void requireWalletAccessThrowsWhenMissing() {
    when(walletRepository.findById(99L)).thenReturn(Optional.empty());
    when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
    assertThrows(WalletNotFoundException.class, () -> walletService.requireWalletAccess(99L));
  }

  @Test
  void updateSettingsPersistsFlags() {
    WalletSettingsRequest request = new WalletSettingsRequest(false, true);
    when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
    when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
    when(walletRepository.save(wallet)).thenReturn(wallet);

    WalletResponse response = walletService.updateSettings(1L, request);

    assertThat(response.activeForShopping()).isFalse();
    assertThat(response.activeForWithdraw()).isTrue();
    verify(walletRepository).save(wallet);
  }
}
