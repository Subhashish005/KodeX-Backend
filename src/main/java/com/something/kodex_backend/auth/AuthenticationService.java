package com.something.kodex_backend.auth;

import com.something.kodex_backend.error.UserAlreadyExistsException;
import com.something.kodex_backend.token.Token;
import com.something.kodex_backend.token.TokenRepository;
import com.something.kodex_backend.token.TokenType;
import com.something.kodex_backend.user.Role;
import com.something.kodex_backend.user.User;
import com.something.kodex_backend.user.UserRepository;
import com.something.kodex_backend.utils.ModelMapper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.MissingRequestCookieException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtAuthenticationUtil jwtAuthenticationUtil;
  private final UserRepository userRepository;
  private final ModelMapper modelMapper;
  private final PasswordEncoder passwordEncoder;
  private final TokenRepository tokenRepository;

  public ResponseEntity<Map<String, String>> login(LoginRequestDto loginRequestDto) {
    Authentication authentication = authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        loginRequestDto.getUsername(),
        loginRequestDto.getPassword()
      )
    );

    User user = (User) authentication.getPrincipal();

    String accessToken = jwtAuthenticationUtil.generateAccessToken(user);
    String refreshToken = jwtAuthenticationUtil.generateRefreshToken(user);

    saveUserJWTRefreshToken(user, refreshToken);

    ResponseCookie responseCookie =
      ResponseCookie
        .from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(false)    // needed for http, change to true for https
        .path("/api/v1")  // this cookie will only be sent to paths which start with this pattern
        .sameSite("Strict")
        .maxAge(jwtAuthenticationUtil.getJwtRefreshExpiration())
        .build();

    return ResponseEntity.ok()
           .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
           .body(Map.of("access_token", accessToken));
  }

  public SignupResponseDto signup(SignupRequestDto signupRequestDto) {
    User user = userRepository.findByUsername(signupRequestDto.getUsername()).orElse(null);

    if(user != null)
      throw new UserAlreadyExistsException("User already exists with username: " + signupRequestDto.getUsername());

    user = userRepository.findByEmail(signupRequestDto.getEmail()).orElse(null);

    if(user != null)
      throw new UserAlreadyExistsException("User already exists with email: " + signupRequestDto.getEmail());

    user = userRepository.save(User.builder()
      .username(signupRequestDto.getUsername())
      .email(signupRequestDto.getEmail())
      .password(passwordEncoder.encode(signupRequestDto.getPassword()))
      .role(Role.valueOf(signupRequestDto.getRole()))
      .build());

    return modelMapper.toSignupResponseDto(user);
  }

  public ResponseEntity<Map<String, String>> renewAccessToken(
    String refreshToken
  ) throws JwtException, NoSuchElementException, NoSuchMethodException, MissingRequestCookieException {
    if(refreshToken == null) {
      Method method = AuthenticationService.class.getMethod("renewAccessToken", String.class);

      throw new MissingRequestCookieException("refresh_token", new MethodParameter(method, 0));
    }

    Token token = tokenRepository.findByValue(refreshToken).orElse(null);

    if(token != null && token.isRevoked()) {
      throw new JwtException("Provided Refresh token is not valid!");
    }

    String username = jwtAuthenticationUtil.getUsernameFromToken(refreshToken);

    if(username == null) {
      throw new UsernameNotFoundException("No username found in the provided refresh token!");
    }

    // TODO: make a custom exception for this
    User user = userRepository.findByUsername(username).orElseThrow();

    if(!jwtAuthenticationUtil.isRefreshTokenValid(refreshToken, user)) {
      throw new JwtException("Provided Refresh token is not valid!");
    }

    final String accessToken = jwtAuthenticationUtil.generateAccessToken(user);

    return ResponseEntity.ok()
      .body(Map.of("access_token", accessToken));
  }

  private void saveUserJWTRefreshToken(User user, String refreshToken) {
    tokenRepository.save(
      Token.builder()
        .value(refreshToken)
        .type(TokenType.JWT_REFRESH)
        .revoked(false)
        .user(user)
        .build()
    );
  }

}