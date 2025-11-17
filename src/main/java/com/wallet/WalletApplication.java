package com.wallet;

import com.wallet.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class WalletApplication {

  public static void main(String[] args) {
    SpringApplication.run(WalletApplication.class, args);
  }
}
