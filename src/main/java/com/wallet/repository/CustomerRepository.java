package com.wallet.repository;

import com.wallet.model.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  Optional<Customer> findByTckn(String tckn);
}
