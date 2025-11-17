package com.wallet.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String issuer;
  private long accessTokenValidityMs;
  private long refreshTokenValidityMs;
  private String secret;

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public long getAccessTokenValidityMs() {
    return accessTokenValidityMs;
  }

  public void setAccessTokenValidityMs(long accessTokenValidityMs) {
    this.accessTokenValidityMs = accessTokenValidityMs;
  }

  public long getRefreshTokenValidityMs() {
    return refreshTokenValidityMs;
  }

  public void setRefreshTokenValidityMs(long refreshTokenValidityMs) {
    this.refreshTokenValidityMs = refreshTokenValidityMs;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }
}
