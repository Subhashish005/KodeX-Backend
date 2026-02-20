package com.something.kodex_backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @PostMapping("/auth/login")
  public ResponseEntity<Map<String, String>> login(
    @RequestBody LoginRequestDto loginRequestDto
  ) {
    return authenticationService.login(loginRequestDto);
  }

  @PostMapping("/auth/signup")
  public ResponseEntity<SignupResponseDto> signup(
    @RequestBody SignupRequestDto signupRequestDto
  ) {
    return ResponseEntity.ok(authenticationService.signup(signupRequestDto));
  }

  // TODO: make a logout method
  // see .logout() in securityConfig

  @GetMapping("/renew-access-token")
  public ResponseEntity<Map<String, String>> renewAccessToken(
    @CookieValue("refresh_token") String refreshToken
  ) throws NoSuchMethodException, MissingRequestCookieException {
    return authenticationService.renewAccessToken(refreshToken);
  }

}