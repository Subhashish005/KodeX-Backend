package com.something.kodex_backend.user;

import com.something.kodex_backend.auth.AuthenticationService;
import com.something.kodex_backend.auth.JwtAuthenticationUtil;
import com.something.kodex_backend.token.Token;
import com.something.kodex_backend.token.TokenRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.MissingRequestCookieException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final TokenRepository tokenRepository;
  private final JwtAuthenticationUtil jwtAuthenticationUtil;

  public ResponseEntity<?> deleteUser(
    String refreshToken
  ) throws JwtException, NoSuchElementException, MissingRequestCookieException, NoSuchMethodException {
    if(refreshToken == null) {
      // TODO: throw some type of exception here

      Method method = AuthenticationService.class.getMethod("renewAccessToken", String.class);

      throw new MissingRequestCookieException("refresh_token", new MethodParameter(method, 0));
    }

    Token token = tokenRepository.findByValue(refreshToken).orElse(null);

    if(token != null && token.isRevoked()) {
      throw new JwtException("Provided Refresh token is not valid!");
    }

    String username = jwtAuthenticationUtil.getUsernameFromToken(refreshToken);

    // TODO: convert nest into guard clause for better readability
    if(username != null) {
      User user = userRepository.findByUsername(username).orElseThrow();

      if(jwtAuthenticationUtil.isRefreshTokenValid(refreshToken, user)) {
        // what about the access token?
        // we are removing user from database it's fine I guess?
        revokeUserJWTRefreshToken(user);

        userRepository.delete(user);
      } else {
        throw new JwtException("Provided Refresh token is not valid!");
      }
    } else {
      throw new UsernameNotFoundException("No user found with the associated username!");
    }

    ResponseCookie responseCookie =
      ResponseCookie
        .from("refresh_token", null)
        .httpOnly(true)
        .secure(false)    // needed for http, change to true for https
        .path("/api/v1/auth")
        .sameSite("Strict")
        .maxAge(0) // to remove the refresh cookie
        .build();

    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
      .build();
  }

  private void revokeUserJWTRefreshToken(User user) {
    List<Token> tokens =
      tokenRepository.findAllValidJWTRefreshTokensByUserId(user.getId()).orElse(null);

    if(tokens == null) return;

    tokens.forEach(t -> t.setRevoked(true));

    tokenRepository.saveAll(tokens);
  }
}