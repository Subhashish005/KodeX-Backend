package com.something.kodex_backend.auth;

import com.something.kodex_backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationUtil {

  @Value("${jwt.secretkey}")
  private String jwtSecretKey;

  @Value("${jwt.access.expiration}")
  private long jwtAccessExpiration;

  @Getter
  @Value("${jwt.refresh.expiration}")
  private long jwtRefreshExpiration;

  // convert secret key into signing key
  private SecretKey getSigningKey() {
    return Keys
      .hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<> ();
    claims.put("name", user.getUsername());
    claims.put("role", user.getRole().toString());

    return generateToken(user, claims, jwtAccessExpiration);
  }

  public String generateRefreshToken(User user) {
    final String sigHash =
      DigestUtils.md5DigestAsHex((user.getUsername() + user.getId()).getBytes(StandardCharsets.UTF_8));

    Map<String, Object> claims = new HashMap<> ();
    claims.put("name", user.getUsername());
    claims.put("sig_hash", sigHash);

    return generateToken(user, claims, jwtRefreshExpiration);
  }

  public String getUsernameFromToken(String token) {
    return extractClaim(token, (claims -> (String) claims.get("name")));
  }

  private String generateToken(
    User user,
    Map<String, Object> claims,
    long expiration
  ) {
    return buildToken(user, claims, expiration);
  }

  private String buildToken(
    User user,
    Map<String, Object> claims,
    long expiration
  ) {
    return Jwts.builder()
      .header()
      .add("typ", "JWT")
      .and()
      .subject(user.getId().toString())
      .claims(claims)
      .issuedAt(new Date(System.currentTimeMillis()))
      .expiration(new Date(System.currentTimeMillis() + expiration))
      .signWith(getSigningKey())
      .compact();
  }

  // A bit advanced thing
  private <R> R extractClaim(
    String token, 
    Function<Claims, R> claimsResolver
  ) {
    final Claims claims = extractAllClaims(token);

    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
      // before extracting any claim/s verify this token first
      .verifyWith(getSigningKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  public boolean isAccessTokenValid(String token, User user) {
    final Claims claims = extractAllClaims(token);

    // I think checking role is redundant
    return !isTokenExpired(token) &&
      claims.get("role").equals(user.getRole().toString());
  }

  public boolean isRefreshTokenValid(String token, User user) {
    final String computedSigHash =
      DigestUtils.md5DigestAsHex((user.getUsername() + user.getId()).getBytes(StandardCharsets.UTF_8));
    final Claims claims = extractAllClaims(token);

    return !isTokenExpired(token) &&
      claims.get("sig_hash").equals(computedSigHash);
  }

  private boolean isTokenExpired(String token) {
    // probably throw an exception here for better clarity
    return extractExpiration(token)
      .before(new Date(System.currentTimeMillis()));
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

}