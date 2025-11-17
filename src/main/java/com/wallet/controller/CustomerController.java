package com.wallet.controller;

import com.wallet.dto.response.CustomerResponse;
import com.wallet.service.CustomerService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping("/me")
  public ResponseEntity<CustomerResponse> me() {
    return ResponseEntity.ok(customerService.getCurrentCustomerProfile());
  }

  @GetMapping
  public ResponseEntity<List<CustomerResponse>> all() {
    return ResponseEntity.ok(customerService.getCustomers());
  }
}
