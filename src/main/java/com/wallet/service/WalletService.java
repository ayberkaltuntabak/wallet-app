package com.wallet.service;

import com.wallet.dto.request.WalletCreateRequest;
import com.wallet.dto.request.WalletSettingsRequest;
import com.wallet.dto.response.WalletResponse;
import com.wallet.enums.Currency;
import com.wallet.enums.UserRole;
import com.wallet.exception.UnauthorizedOperationException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.model.Customer;
import com.wallet.model.Wallet;
import com.wallet.repository.WalletRepository;
import com.wallet.service.policy.WalletAccessPolicy;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final CurrentCustomerService currentCustomerService;
  private final WalletAccessPolicy walletAccessPolicy;
  private final CustomerService customerService;

  public WalletService(
      WalletRepository walletRepository,
      CurrentCustomerService currentCustomerService,
      WalletAccessPolicy walletAccessPolicy,
      CustomerService customerService) {
    this.walletRepository = walletRepository;
    this.currentCustomerService = currentCustomerService;
    this.walletAccessPolicy = walletAccessPolicy;
    this.customerService = customerService;
  }

  @Transactional
  public WalletResponse createWallet(WalletCreateRequest request) {
    Customer current = currentCustomerService.getCurrentCustomer();
    Customer owner = resolveOwnerForCreation(request.customerId(), current);
    Wallet wallet = new Wallet();
    wallet.setCustomer(owner);
    wallet.setWalletName(request.walletName());
    wallet.setCurrency(request.currency());
    wallet.setActiveForShopping(request.activeForShopping());
    wallet.setActiveForWithdraw(request.activeForWithdraw());
    Wallet saved = walletRepository.save(wallet);
    return toResponse(saved);
  }

  public List<WalletResponse> listWallets(Optional<Long> customerId, Optional<Currency> currency) {
    Customer current = currentCustomerService.getCurrentCustomer();
    Long ownerId = resolveOwner(customerId, current);
    List<Wallet> wallets;
    if (ownerId == null) {
      wallets = walletRepository.findAll();
    } else if (currency.isPresent()) {
      wallets = walletRepository.findByCustomerIdAndCurrency(ownerId, currency.get());
    } else {
      wallets = walletRepository.findByCustomerId(ownerId);
    }
    return wallets.stream().map(this::toResponse).toList();
  }

  public WalletResponse getWallet(Long walletId) {
    Wallet wallet = requireWalletAccess(walletId);
    return toResponse(wallet);
  }

  @Transactional
  public WalletResponse updateSettings(Long walletId, WalletSettingsRequest request) {
    Wallet wallet = requireWalletAccess(walletId);
    wallet.setActiveForShopping(request.activeForShopping());
    wallet.setActiveForWithdraw(request.activeForWithdraw());
    return toResponse(walletRepository.save(wallet));
  }

  public Wallet requireWalletAccess(Long walletId) {
    Customer current = currentCustomerService.getCurrentCustomer();
    Wallet wallet =
        walletRepository
            .findById(walletId)
            .orElseThrow(() -> new WalletNotFoundException(walletId));
    walletAccessPolicy.ensureCanAccess(current, wallet);
    return wallet;
  }

  private Long resolveOwner(Optional<Long> requestedCustomerId, Customer current) {
    if (walletAccessPolicy.canViewAll(current)) {
      return requestedCustomerId.orElse(null);
    }
    return current.getId();
  }

  private Customer resolveOwnerForCreation(Long requestedCustomerId, Customer actor) {
    if (requestedCustomerId == null || actor.getRole() == UserRole.CUSTOMER) {
      if (requestedCustomerId != null && !actor.getId().equals(requestedCustomerId)) {
        throw new UnauthorizedOperationException("Customers cannot create wallets for others");
      }
      return actor;
    }
    return customerService.ensureCustomerExists(requestedCustomerId);
  }

  private WalletResponse toResponse(Wallet wallet) {
    return new WalletResponse(
        wallet.getId(),
        wallet.getCustomer().getId(),
        wallet.getWalletName(),
        wallet.getCurrency(),
        wallet.isActiveForShopping(),
        wallet.isActiveForWithdraw(),
        wallet.getBalance(),
        wallet.getUsableBalance());
  }
}
