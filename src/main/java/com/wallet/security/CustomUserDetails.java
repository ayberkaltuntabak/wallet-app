package com.wallet.security;

import com.wallet.enums.UserRole;
import com.wallet.model.Customer;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

  private final Customer customer;

  public CustomUserDetails(Customer customer) {
    this.customer = customer;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(customer.getRole().name()));
  }

  @Override
  public String getPassword() {
    return customer.getPassword();
  }

  @Override
  public String getUsername() {
    return customer.getTckn();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public Long getCustomerId() {
    return customer.getId();
  }

  public UserRole getRole() {
    return customer.getRole();
  }
}
