package com.something.kodex_backend.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
    return authenticationService.login(loginRequestDto);
  }

  @PostMapping("/signup")
  public ResponseEntity<SignupResponseDto> signup(@RequestBody SignupRequestDto signupRequestDto) {
    return ResponseEntity.ok(authenticationService.signup(signupRequestDto));
  }

  // TODO: make a logout method
  // see .logout() in securityConfig

  @GetMapping("/renew-access-token")
  public ResponseEntity<?> renewAccessToken(
    @CookieValue("refresh_token") String refreshToken
  ) throws NoSuchMethodException, MissingRequestCookieException {
    return authenticationService.renewAccessToken(refreshToken);
  }

}
