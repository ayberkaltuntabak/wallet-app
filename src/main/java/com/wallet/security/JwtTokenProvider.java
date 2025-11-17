package com.wallet.security;

import com.wallet.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final JwtProperties properties;

  public JwtTokenProvider(JwtProperties properties) {
    this.properties = properties;
  }

  public String generateAccessToken(Authentication authentication) {
    return generateToken(
        authentication.getName(),
        extractRole(authentication),
        properties.getAccessTokenValidityMs());
  }

  public String generateRefreshToken(Authentication authentication) {
    return generateToken(
        authentication.getName(),
        extractRole(authentication),
        properties.getRefreshTokenValidityMs());
  }

  public boolean validate(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String extractUsername(String token) {
    return claims(token).getSubject();
  }

  public UserRole extractRole(String token) {
    String role = claims(token).get("role", String.class);
    return UserRole.valueOf(role);
  }

  private String generateToken(String subject, String role, long validityMs) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + validityMs);
    return Jwts.builder()
        .setSubject(subject)
        .claim("role", role)
        .setIssuer(properties.getIssuer())
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(signingKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private Claims claims(String token) {
    return Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token).getBody();
  }

  private Key signingKey() {
    return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  private String extractRole(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse(UserRole.CUSTOMER.name());
  }
}
